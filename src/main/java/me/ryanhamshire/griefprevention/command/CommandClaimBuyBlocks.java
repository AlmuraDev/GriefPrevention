/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
 * Copyright (c) Ryan Hamshire
 * Copyright (c) bloodmc
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package me.ryanhamshire.griefprevention.command;

import com.google.common.collect.ImmutableMap;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.math.BigDecimal;
import java.util.Optional;

public class CommandClaimBuyBlocks implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        if (GriefPreventionPlugin.getGlobalConfig().getConfig().economy.economyMode) {
            src.sendMessage(Text.of(TextColors.RED, "This command is not available while server is in economy mode."));
            return CommandResult.success();
        }

        Player player;
        try {
            player = GriefPreventionPlugin.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        // if economy is disabled, don't do anything
        if (!GriefPreventionPlugin.instance.economyService.isPresent()) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.economyNotInstalled.toText());
            return CommandResult.success();
        }

        Account playerAccount = GriefPreventionPlugin.instance.economyService.get().getOrCreateAccount(player.getUniqueId()).orElse(null);
        if (playerAccount == null) {
            final Text message = GriefPreventionPlugin.instance.messageData.economyUserNotFound
                    .apply(ImmutableMap.of(
                    "user", player.getName())).build();
            GriefPreventionPlugin.sendMessage(player, message);
            return CommandResult.success();
        }

        final GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        if (playerData.optionEconomyClaimBlockCost == 0 && playerData.optionEconomyClaimBlockSell == 0) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.economyBuySellNotConfigured.toText());
            return CommandResult.success();
        }

        // if purchase disabled, send error message
        if (playerData.optionEconomyClaimBlockCost == 0) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.economyOnlySellBlocks.toText());
            return CommandResult.success();
        }

        Optional<Integer> blockCountOpt = ctx.getOne("numberOfBlocks");
        double balance = playerAccount.getBalance(GriefPreventionPlugin.instance.economyService.get().getDefaultCurrency()).doubleValue();
        // if no parameter, just tell player cost per block and balance
        if (!blockCountOpt.isPresent()) {
            final Text message = GriefPreventionPlugin.instance.messageData.economyBlockPurchaseCost
                    .apply(ImmutableMap.of(
                    "cost", playerData.optionEconomyClaimBlockCost,
                    "balance", balance)).build();
            GriefPreventionPlugin.sendMessage(player, message);
            return CommandResult.success();
        } else {
            // try to parse number of blocks
            int blockCount = blockCountOpt.get();

            if (blockCount <= 0) {
                GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.economyBuyInvalidBlockCount.toText());
                return CommandResult.success();
            }

            double totalCost = blockCount * playerData.optionEconomyClaimBlockCost;
            // attempt to withdraw cost
            try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                Sponge.getCauseStackManager().addContext(GriefPreventionPlugin.PLUGIN_CONTEXT, GriefPreventionPlugin.instance);
                TransactionResult transactionResult = playerAccount.withdraw
                    (GriefPreventionPlugin.instance.economyService.get().getDefaultCurrency(), BigDecimal.valueOf(totalCost),
                        Sponge.getCauseStackManager().getCurrentCause());

                if (transactionResult.getResult() != ResultType.SUCCESS) {
                    final Text message = GriefPreventionPlugin.instance.messageData.economyWithdrawError
                        .apply(ImmutableMap.of(
                            "reason", transactionResult.getResult().name())).build();
                    GriefPreventionPlugin.sendMessage(player, message);
                    return CommandResult.success();
                }
            }
            // add blocks
            playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() + blockCount);
            playerData.getStorageData().save();

            final Text message = GriefPreventionPlugin.instance.messageData.economyBlocksPurchaseConfirmation
                    .apply(ImmutableMap.of(
                    "cost", totalCost,
                    "remaining-blocks", playerData.getRemainingClaimBlocks())).build();
            // inform player
            GriefPreventionPlugin.sendMessage(player, message);
        }
        return CommandResult.success();
    }
}

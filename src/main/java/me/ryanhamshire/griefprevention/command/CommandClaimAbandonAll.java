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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.event.GPDeleteClaimEvent;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.math.BigDecimal;

public class CommandClaimAbandonAll implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPreventionPlugin.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        // count claims
        final GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        int originalClaimCount = playerData.getInternalClaims().size();

        // check count
        if (originalClaimCount == 0) {
            try {
                throw new CommandException(GriefPreventionPlugin.instance.messageData.claimNoClaims.toText());
            } catch (CommandException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }
        try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            Sponge.getCauseStackManager().pushCause(src);
            GPDeleteClaimEvent.Abandon event = new GPDeleteClaimEvent.Abandon(ImmutableList.copyOf(playerData.getInternalClaims()));
            Sponge.getEventManager().post(event);
            if (event.isCancelled()) {
                player.sendMessage(Text.of(TextColors.RED, event.getMessage().orElse(Text.of("Could not abandon claim. A plugin has denied it."))));
                return CommandResult.success();
            }
        }

        final boolean econMode = GriefPreventionPlugin.getGlobalConfig().getConfig().economy.economyMode;
        double refund = 0;
        // adjust claim blocks
        for (Claim claim : playerData.getInternalClaims()) {
            // remove all context permissions
            player.getSubjectData().clearPermissions(ImmutableSet.of(claim.getContext()));
            if (claim.isSubdivision() || claim.isAdminClaim() || claim.isWilderness()) {
                continue;
            }
            if (econMode) {
                refund += claim.getClaimBlocks() * playerData.optionAbandonReturnRatioBasic;
            } else {
                playerData.setAccruedClaimBlocks(playerData.getAccruedClaimBlocks() - ((int) Math.ceil(claim.getClaimBlocks() * (1 - playerData.optionAbandonReturnRatioBasic))));
            }
        }

        // delete them
        GriefPreventionPlugin.instance.dataStore.deleteClaimsForPlayer(player.getUniqueId());

        if (econMode) {
            final Account playerAccount = GriefPreventionPlugin.instance.economyService.get().getOrCreateAccount(playerData.playerID).orElse(null);
            if (playerAccount == null) {
                return CommandResult.success();
            }

            final Currency defaultCurrency = GriefPreventionPlugin.instance.economyService.get().getDefaultCurrency();
            final TransactionResult result = playerAccount.deposit(defaultCurrency, BigDecimal.valueOf(refund), Sponge.getCauseStackManager().getCurrentCause());
            if (result.getResult() == ResultType.SUCCESS) {
                final Text message = GriefPreventionPlugin.instance.messageData.economyClaimAbandonSuccess
                        .apply(ImmutableMap.of(
                        "refund", Text.of(refund)
                )).build();
                GriefPreventionPlugin.sendMessage(player, message);
            }
        } else {
            // tell the player how many claim blocks he has left
            int remainingBlocks = playerData.getRemainingClaimBlocks();
            final Text message = GriefPreventionPlugin.instance.messageData.claimAbandonSuccess
                    .apply(ImmutableMap.of(
                    "remaining-blocks", Text.of(remainingBlocks)
            )).build();
            GriefPreventionPlugin.sendMessage(player, message);
        }
        // revert any current visualization
        playerData.revertActiveVisual(player);

        return CommandResult.success();
    }
}

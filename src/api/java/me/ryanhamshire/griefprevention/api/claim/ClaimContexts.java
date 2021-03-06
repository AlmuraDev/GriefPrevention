/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
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
package me.ryanhamshire.griefprevention.api.claim;

import org.spongepowered.api.service.context.Context;

import java.util.Arrays;
import java.util.List;

/**
 * Contains a list of all possible default and override contexts used.
 */
public class ClaimContexts {

    public static final Context ADMIN_DEFAULT_CONTEXT = new Context("gp_claim_defaults", "ADMIN");
    public static final Context ADMIN_OVERRIDE_CONTEXT = new Context("gp_claim_overrides", "ADMIN");
    public static final Context BASIC_DEFAULT_CONTEXT = new Context("gp_claim_defaults", "BASIC");
    public static final Context BASIC_OVERRIDE_CONTEXT = new Context("gp_claim_overrides", "BASIC");
    public static final Context TOWN_DEFAULT_CONTEXT = new Context("gp_claim_defaults", "TOWN");
    public static final Context TOWN_OVERRIDE_CONTEXT = new Context("gp_claim_overrides", "TOWN");
    public static final Context WILDERNESS_OVERRIDE_CONTEXT = new Context("gp_claim_overrides", "WILDERNESS");
    public static final Context WILDERNESS_DEFAULT_CONTEXT = new Context("gp_claim_defaults", "WILDERNESS");
    public static final List<Context> CONTEXT_LIST = Arrays.asList(
            ADMIN_DEFAULT_CONTEXT, ADMIN_OVERRIDE_CONTEXT,
            BASIC_DEFAULT_CONTEXT, BASIC_OVERRIDE_CONTEXT,
            TOWN_DEFAULT_CONTEXT, TOWN_OVERRIDE_CONTEXT,
            WILDERNESS_OVERRIDE_CONTEXT, WILDERNESS_DEFAULT_CONTEXT);
}

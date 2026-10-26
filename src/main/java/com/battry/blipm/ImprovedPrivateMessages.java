package com.battry.blipm;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ImprovedPrivateMessages — common entrypoint.
 *
 * This mod is client-only in practice, but Fabric's dual-entrypoint pattern
 * requires a main entrypoint when environment is "*" in fabric.mod.json.
 *
 * All real logic lives in ImprovedPrivateMessagesClient (client entrypoint).
 */
public class ImprovedPrivateMessages implements ModInitializer {

    public static final String MOD_ID = "improved-private-messages";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Common init — intentionally minimal.
        // No server-side logic needed; this mod is a client UI layer only.
        LOGGER.info("[BLIPM] BL Improved Private Messages loaded.");
    }
}

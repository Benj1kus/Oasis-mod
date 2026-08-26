package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.config.OsirisRealmConfig;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.AzumaalEntity;
import com.benji.oasiso.common.entity.EyelidEntity;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

public final class AzumaalDefenseManager {

    private static final double SEARCH_RANGE = 96.0D;

    private final AzumaalEntity boss;

    public AzumaalDefenseManager(AzumaalEntity boss) {
        this.boss = boss;
    }

    public boolean activate(ServerLevel level) {
        if (isActive(level)) {
            return false;
        }

        int spawned = 0;
        for (int i = 0; i < OsirisRealmConfig.AZUMAAL_DEFENSE_EYELID_COUNT.get(); i++) {
            EyelidEntity eyelid = Oasiso.EYELID.get().create(level);
            if (eyelid == null) {
                continue;
            }

            eyelid.initializeOrbit(boss, i);
            if (level.addFreshEntity(eyelid)) {
                spawned++;
            }
        }

        if (spawned <= 0) {
            return false;
        }
        boss.setDefending(true);
        return true;
    }

    public void tick(ServerLevel level) {
        if (!boss.isDefending()) {
            return;
        }

        if (!boss.isAlive()) {
            boss.setDefending(false);
            return;
        }

        if (hasOwnedEyelids(level)) {
            return;
        }
        boss.setDefending(false);
    }

    public boolean isActive(ServerLevel level) {
        return boss.isDefending() || hasOwnedEyelids(level);
    }

    private boolean hasOwnedEyelids(ServerLevel level) {
        List<EyelidEntity> eyelids = level.getEntitiesOfClass(EyelidEntity.class, boss.getBoundingBox().inflate(SEARCH_RANGE), eyelid -> eyelid.isOwnedBy(boss));

        return !eyelids.isEmpty();
    }

    public void reset() {
        boss.setDefending(false);
    }
}
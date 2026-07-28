package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class DoumPalmChestBoatEntity extends ChestBoat {
    public DoumPalmChestBoatEntity(EntityType<? extends ChestBoat> type, Level level) {
        super(type, level);
    }

    public DoumPalmChestBoatEntity(Level level, double x, double y, double z) {
        this(Oasiso.DOUM_PALM_CHEST_BOAT.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    public Item getDropItem() {
        return Oasiso.DOUM_PALM_CHEST_BOAT_ITEM.get();
    }
}
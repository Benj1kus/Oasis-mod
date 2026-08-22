package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class DoumPalmBoatEntity extends Boat {
    public DoumPalmBoatEntity(EntityType<? extends Boat> type, Level level) {
        super(type, level);
    }

    public DoumPalmBoatEntity(Level level, double x, double y, double z) {
        this(Oasiso.DOUM_PALM_BOAT.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    public Item getDropItem() {
        return Oasiso.DOUM_PALM_BOAT_ITEM.get();
    }
}
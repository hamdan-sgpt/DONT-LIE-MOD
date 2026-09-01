package com.corazon.corazonmod.init;

import com.corazon.corazonmod.CorazonMod;
import com.corazon.corazonmod.entity.MoneyPouchEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CorazonMod.MOD_ID);

    public static final RegistryObject<EntityType<MoneyPouchEntity>> MONEY_POUCH =
            ENTITY_TYPES.register("money_pouch",
                    () -> EntityType.Builder.<MoneyPouchEntity>of(MoneyPouchEntity::new, MobCategory.MISC)
                            .sized(0.3F, 0.3F)
                            .clientTrackingRange(10)
                            .updateInterval(20)
                            .build("money_pouch"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}

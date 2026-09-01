package com.corazon.corazonmod.init;

import com.corazon.corazonmod.CorazonMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = 
            DeferredRegister.create(ForgeRegistries.ITEMS, CorazonMod.MOD_ID);

    // Starter & Don't Lie Mod Items
    public static final RegistryObject<Item> CORAZON_COIN = ITEMS.register("corazon_coin",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CORAZON_SWORD = ITEMS.register("corazon_sword",
            () -> new SwordItem(Tiers.NETHERITE, 7, -2.4F, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> MONEY_POUCH = ITEMS.register("money_pouch",
            () -> new Item(new Item.Properties().stacksTo(1).fireResistant()));

    public static final RegistryObject<Item> MAFIA_DAGGER = ITEMS.register("mafia_dagger",
            () -> new SwordItem(Tiers.DIAMOND, 5, -2.0F, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

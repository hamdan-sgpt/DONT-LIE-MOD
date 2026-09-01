package com.corazon.corazonmod.init;

import com.corazon.corazonmod.CorazonMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CorazonMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> CORAZON_TAB = CREATIVE_MODE_TABS.register("corazon_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.MONEY_POUCH.get()))
                    .title(Component.translatable("creativetab.corazon_tab"))
                    .displayItems((parameters, output) -> {
                        // Game Items (Don't Lie)
                        output.accept(ModItems.MONEY_POUCH.get());
                        output.accept(ModItems.MAFIA_DAGGER.get());
                        
                        // General Mod Items & Blocks
                        output.accept(ModItems.CORAZON_COIN.get());
                        output.accept(ModItems.CORAZON_SWORD.get());
                        output.accept(ModBlocks.CORAZON_BLOCK.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}

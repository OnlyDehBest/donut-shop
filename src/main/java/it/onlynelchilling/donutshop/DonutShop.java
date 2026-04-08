package it.onlynelchilling.donutshop;

import co.aikar.commands.PaperCommandManager;
import it.onlynelchilling.donutshop.command.ShopCommand;
import it.onlynelchilling.donutshop.config.MainConfig;
import it.onlynelchilling.donutshop.config.MessagesConfig;
import it.onlynelchilling.donutshop.config.ShopConfig;
import it.onlynelchilling.donutshop.config.SoundConfig;
import it.onlynelchilling.donutshop.gui.GuiListener;
import it.onlynelchilling.donutshop.shop.VaultHook;
import org.bukkit.plugin.java.JavaPlugin;

public class DonutShop extends JavaPlugin {

    private static DonutShop instance;
    private MainConfig mainConfig;
    private ShopConfig shopConfig;
    private MessagesConfig messagesConfig;
    private SoundConfig soundConfig;
    private VaultHook vaultHook;

    @Override
    public void onEnable() {
        instance = this;

        try {
            vaultHook = new VaultHook();
        } catch (IllegalStateException e) {
            getLogger().severe(e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        mainConfig = new MainConfig(this);
        messagesConfig = new MessagesConfig(this);
        soundConfig = new SoundConfig(this);
        shopConfig = new ShopConfig(this, mainConfig);

        PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.registerCommand(new ShopCommand());

        getServer().getPluginManager().registerEvents(new GuiListener(), this);
    }

    public static DonutShop getInstance() {
        return instance;
    }

    public MainConfig getMainConfig() {
        return mainConfig;
    }

    public ShopConfig getShopConfig() {
        return shopConfig;
    }

    public MessagesConfig getMessagesConfig() {
        return messagesConfig;
    }

    public SoundConfig getSoundConfig() {
        return soundConfig;
    }

    public VaultHook getVaultHook() {
        return vaultHook;
    }
}

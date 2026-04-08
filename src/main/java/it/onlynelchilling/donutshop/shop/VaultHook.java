package it.onlynelchilling.donutshop.shop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {

    private Economy economy;

    public VaultHook() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
        }
        if (economy == null) {
            throw new IllegalStateException("Vault economy provider not found");
        }
    }

    public Economy getEconomy() {
        return economy;
    }
}


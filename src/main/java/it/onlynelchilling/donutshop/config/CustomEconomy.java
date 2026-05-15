package it.onlynelchilling.donutshop.config;

public record CustomEconomy(
        String id,
        String displayName,
        String balancePlaceholder,
        String takeCommand,
        String giveCommand
) {
    public String formatPrice(double price) {
        return Math.round(price) + " " + displayName;
    }
}


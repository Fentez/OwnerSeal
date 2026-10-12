package ua.fentez.ownerseal;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public final class OwnerSeal extends JavaPlugin {

    public NamespacedKey ownerSealKey;
    public NamespacedKey authorUuidKey;
    public NamespacedKey authorNameKey;

    private ItemStack sealItemTemplate;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        ownerSealKey = new NamespacedKey(this, "owner_seal");
        authorUuidKey = new NamespacedKey(this, "author_uuid");
        authorNameKey = new NamespacedKey(this, "author_name");

        createSealItem();
        registerRecipe();

        getServer().getPluginManager().registerEvents(new SealListener(this), this);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("ownerseal")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("ownerseal.admin")) {
                    reloadConfig();
                    createSealItem();

                    Bukkit.removeRecipe(new NamespacedKey(this, "owner_seal_recipe"));
                    registerRecipe();

                    String msg = getConfig().getString("messages.reloaded", "<green>OwnerSeal configuration successfully reloaded!</green>");
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(msg));
                    return true;
                } else {
                    String msg = getConfig().getString("messages.no-permission", "<red>You do not have permission to use this command.</red>");
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(msg));
                    return true;
                }
            } else if (args.length > 0 && (args[0].equalsIgnoreCase("unseal") || args[0].equalsIgnoreCase("remove"))) {
                if (sender.hasPermission("ownerseal.admin")) {
                    if (!(sender instanceof Player player)) {
                        String msg = getConfig().getString("messages.only-player", "<red>Only players can use this command!</red>");
                        sender.sendMessage(MiniMessage.miniMessage().deserialize(msg));
                        return true;
                    }

                    ItemStack item = player.getInventory().getItemInMainHand();
                    if (item.isEmpty() || !item.hasItemMeta()) {
                        String msg = getConfig().getString("messages.hold-item", "<red>You must hold a sealed item in your main hand!</red>");
                        player.sendMessage(MiniMessage.miniMessage().deserialize(msg));
                        return true;
                    }

                    ItemMeta meta = item.getItemMeta();
                    if (meta == null) return true;

                    PersistentDataContainer pdc = meta.getPersistentDataContainer();
                    if (!pdc.has(authorUuidKey, PersistentDataType.STRING)) {
                        String msg = getConfig().getString("messages.not-sealed", "<red>This item does not have an Owner's Seal!</red>");
                        player.sendMessage(MiniMessage.miniMessage().deserialize(msg));
                        return true;
                    }

                    String authorName = pdc.getOrDefault(authorNameKey, PersistentDataType.STRING, "Unknown");
                    String protectedMsg = getConfig().getString("messages.protected", "<!italic><red>Owner: <italic><author></red>")
                            .replace("<author>", authorName);
                    String copyDenyMsg = getConfig().getString("messages.copy-deny", "<!italic><red>Copying prohibited</red>");

                    Component protectedComp = MiniMessage.miniMessage().deserialize(protectedMsg);
                    Component copyDenyComp = MiniMessage.miniMessage().deserialize(copyDenyMsg);

                    if (meta.hasLore() && meta.lore() != null) {
                        List<Component> lore = new ArrayList<>(meta.lore());
                        lore.remove(protectedComp);
                        lore.remove(copyDenyComp);
                        meta.lore(lore);
                    }

                    pdc.remove(authorUuidKey);
                    pdc.remove(authorNameKey);

                    item.setItemMeta(meta);
                    String msg = getConfig().getString("messages.unsealed", "<green>Owner's Seal successfully removed (Admin)!</green>");
                    player.sendMessage(MiniMessage.miniMessage().deserialize(msg));
                    return true;
                } else {
                    String msg = getConfig().getString("messages.no-permission", "<red>You do not have permission to use this command.</red>");
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(msg));
                    return true;
                }
            } else {
                String msg = getConfig().getString("messages.usage", "<yellow>Usage: /ownerseal <reload|unseal></yellow>");
                sender.sendMessage(MiniMessage.miniMessage().deserialize(msg));
                return true;
            }
        }
        return false;
    }

    public Material getSealMaterial() {
        Material mat = Material.matchMaterial(getConfig().getString("item.material", "ENCHANTED_BOOK"));
        return mat != null ? mat : Material.ENCHANTED_BOOK;
    }

    private void createSealItem() {
        sealItemTemplate = new ItemStack(getSealMaterial());

        String nameStr = getConfig().getString("item.name", "<!italic><gold>Owner's Seal</gold>");
        List<String> loreStrList = getConfig().getStringList("item.lore");
        int maxDurability = getConfig().getInt("item.max-durability", 24);

        Component nameComp = MiniMessage.miniMessage().deserialize(nameStr);
        List<Component> loreCompList = loreStrList.stream()
                .map(s -> MiniMessage.miniMessage().deserialize(s))
                .collect(Collectors.toList());

        sealItemTemplate.editMeta(meta -> {
            meta.displayName(nameComp);
            meta.lore(loreCompList);

            meta.getPersistentDataContainer().set(ownerSealKey, PersistentDataType.BYTE, (byte) 1);
        });

        sealItemTemplate.setData(DataComponentTypes.ITEM_MODEL, Key.key("minecraft:spyglass"));
        sealItemTemplate.setData(DataComponentTypes.MAX_DAMAGE, maxDurability);
        sealItemTemplate.setData(DataComponentTypes.DAMAGE, 0);
    }

    private void registerRecipe() {
        if (!getConfig().getBoolean("recipe.enabled", true)) return;

        NamespacedKey recipeKey = new NamespacedKey(this, "owner_seal_recipe");
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, sealItemTemplate);

        List<String> shape = getConfig().getStringList("recipe.shape");
        if (shape.size() == 3) {
            recipe.shape(shape.get(0), shape.get(1), shape.get(2));
        } else {
            recipe.shape(" H ", "HGH", " S ");
        }

        ConfigurationSection ingredientsSection = getConfig().getConfigurationSection("recipe.ingredients");
        if (ingredientsSection != null) {
            for (String key : ingredientsSection.getKeys(false)) {
                String matStr = getConfig().getString("recipe.ingredients." + key);
                if (matStr != null) {
                    Material mat = Material.matchMaterial(matStr);
                    if (mat != null) {
                        recipe.setIngredient(key.charAt(0), mat);
                    }
                }
            }
        } else {
            recipe.setIngredient('H', Material.HONEYCOMB);
            recipe.setIngredient('G', Material.GOLD_INGOT);
            recipe.setIngredient('S', Material.STICK);
        }

        Bukkit.addRecipe(recipe);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.discoverRecipe(recipeKey);
        }
    }
}

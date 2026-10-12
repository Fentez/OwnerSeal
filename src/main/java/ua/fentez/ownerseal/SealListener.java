package ua.fentez.ownerseal;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.block.Crafter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.CartographyInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings({"UnstableApiUsage", "deprecation", "removal"})
public class SealListener implements Listener {

    private final OwnerSeal plugin;

    public SealListener(OwnerSeal plugin) {
        this.plugin = plugin;
    }

    private Sound getSound(String path, Sound defaultSound) {
        String soundStr = plugin.getConfig().getString("sounds." + path);
        if (soundStr != null) {
            try {
                @SuppressWarnings({"deprecation", "removal"})
                Sound sound = Sound.valueOf(soundStr.toUpperCase());
                return sound;
            } catch (IllegalArgumentException | NoSuchMethodError e) {
                try {
                    Registry<Sound> registry = Registry.SOUNDS;
                    Sound sound = registry.get(NamespacedKey.minecraft(soundStr.toLowerCase()));
                    if (sound != null) return sound;

                    sound = registry.get(NamespacedKey.minecraft(soundStr.toLowerCase().replace("_", ".")));
                    if (sound != null) return sound;
                } catch (Exception ignored) {}
            }
        }
        return defaultSound;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getInventory() instanceof CartographyInventory cartography) {
            if (event.getSlotType() == InventoryType.SlotType.RESULT) {
                ItemStack map = cartography.getItem(0);
                if (map != null && map.hasItemMeta()) {
                    PersistentDataContainer pdc = map.getItemMeta().getPersistentDataContainer();
                    if (pdc.has(plugin.authorUuidKey, PersistentDataType.STRING)) {
                        String authorUuid = pdc.get(plugin.authorUuidKey, PersistentDataType.STRING);
                        if (!player.getUniqueId().toString().equals(authorUuid)) {
                            event.setCancelled(true);
                            String authorName = pdc.getOrDefault(plugin.authorNameKey, PersistentDataType.STRING, "Unknown");
                            String notYoursMsg = plugin.getConfig().getString("messages.not-yours", "<red>Hands off! This item was sealed by <author>!</red>")
                                    .replace("<author>", authorName);
                            player.sendActionBar(MiniMessage.miniMessage().deserialize(notYoursMsg));
                            player.playSound(player.getLocation(), getSound("error", Sound.ENTITY_VILLAGER_NO), 1.0f, 1.0f);
                            return;
                        }
                    }
                }
            }
        }

        if (event.getClick() != ClickType.LEFT) return;

        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        if (cursor.isEmpty()) return;
        if (current == null || current.isEmpty()) return;

        ItemMeta cursorMeta = cursor.getItemMeta();
        if (cursorMeta == null) return;
        PersistentDataContainer cursorPdc = cursorMeta.getPersistentDataContainer();
        if (!cursorPdc.has(plugin.ownerSealKey, PersistentDataType.BYTE)) return;

        if (current.getType() != Material.FILLED_MAP && !current.getType().name().endsWith("_SMITHING_TEMPLATE")) {
            return;
        }

        if (player.hasCooldown(plugin.getSealMaterial())) {
            event.setCancelled(true);
            return;
        }

        ItemMeta targetMeta = current.getItemMeta();
        if (targetMeta == null) return;
        PersistentDataContainer targetPdc = targetMeta.getPersistentDataContainer();

        boolean hasAuthor = targetPdc.has(plugin.authorUuidKey, PersistentDataType.STRING);
        String currentAuthorUuidStr = hasAuthor ? targetPdc.get(plugin.authorUuidKey, PersistentDataType.STRING) : null;
        UUID playerUuid = player.getUniqueId();

        if (!hasAuthor) {
            targetPdc.set(plugin.authorUuidKey, PersistentDataType.STRING, playerUuid.toString());
            targetPdc.set(plugin.authorNameKey, PersistentDataType.STRING, player.getName());

            String protectedMsg = plugin.getConfig().getString("messages.protected", "<!italic><red>Owner: <italic><author></red>")
                    .replace("<author>", player.getName());
            String copyDenyMsg = plugin.getConfig().getString("messages.copy-deny", "<!italic><red>Copying prohibited</red>");

            List<Component> lore = new ArrayList<>();
            if (targetMeta.hasLore()) {
                List<Component> currentLore = targetMeta.lore();
                if (currentLore != null) {
                    lore.addAll(currentLore);
                }
            }
            lore.add(MiniMessage.miniMessage().deserialize(protectedMsg));
            lore.add(MiniMessage.miniMessage().deserialize(copyDenyMsg));
            targetMeta.lore(lore);

            current.setItemMeta(targetMeta);

            if (player.getGameMode() != GameMode.CREATIVE) {
                damageSeal(cursor, player);
            }

            player.playSound(player.getLocation(), getSound("apply-seal", Sound.BLOCK_ANVIL_USE), 1.0f, 1.0f);

            int cooldownTicks = (int) (plugin.getConfig().getDouble("settings.cooldown-seconds", 1.5) * 20);
            player.setCooldown(plugin.getSealMaterial(), cooldownTicks);
            event.setCancelled(true);
            return;
        }

        if (currentAuthorUuidStr != null) {
            if (currentAuthorUuidStr.equals(playerUuid.toString())) {
                targetPdc.remove(plugin.authorUuidKey);
                targetPdc.remove(plugin.authorNameKey);

                String protectedMsg = plugin.getConfig().getString("messages.protected", "<!italic><red>Owner: <italic><author></red>")
                        .replace("<author>", player.getName());
                String copyDenyMsg = plugin.getConfig().getString("messages.copy-deny", "<!italic><red>Copying prohibited</red>");

                Component protectedComp = MiniMessage.miniMessage().deserialize(protectedMsg);
                Component copyDenyComp = MiniMessage.miniMessage().deserialize(copyDenyMsg);

                if (targetMeta.hasLore()) {
                    List<Component> currentLore = targetMeta.lore();
                    if (currentLore != null) {
                        List<Component> lore = new ArrayList<>(currentLore);
                        lore.remove(protectedComp);
                        lore.remove(copyDenyComp);
                        targetMeta.lore(lore);
                    }
                }

                current.setItemMeta(targetMeta);

                damageSeal(cursor, player);

                player.playSound(player.getLocation(), getSound("remove-seal", Sound.BLOCK_AMETHYST_BLOCK_CHIME), 1.0f, 1.0f);

                int cooldownTicks = (int) (plugin.getConfig().getDouble("settings.cooldown-seconds", 1.5) * 20);
                player.setCooldown(plugin.getSealMaterial(), cooldownTicks);
                event.setCancelled(true);
            } else {
                String authorName = targetPdc.getOrDefault(plugin.authorNameKey, PersistentDataType.STRING, "Unknown");
                String notYoursMsg = plugin.getConfig().getString("messages.not-yours", "<red>Hands off! This item was sealed by <author>!</red>")
                        .replace("<author>", authorName);

                player.sendActionBar(MiniMessage.miniMessage().deserialize(notYoursMsg));
                player.playSound(player.getLocation(), getSound("error", Sound.ENTITY_VILLAGER_NO), 1.0f, 1.0f);
                event.setCancelled(true);
            }
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private void damageSeal(ItemStack seal, Player player) {
        Integer maxDamage = seal.getData(DataComponentTypes.MAX_DAMAGE);
        Integer damage = seal.getData(DataComponentTypes.DAMAGE);

        if (maxDamage == null) maxDamage = plugin.getConfig().getInt("item.max-durability", 24);
        if (damage == null) damage = 0;

        damage++;

        if (damage >= maxDamage) {
            seal.setAmount(0);
            player.playSound(player.getLocation(), getSound("break-seal", Sound.ENTITY_ITEM_BREAK), 1.0f, 1.0f);
        } else {
            seal.setData(DataComponentTypes.DAMAGE, damage);
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    PersistentDataContainer pdc = meta.getPersistentDataContainer();
                    if (pdc.has(plugin.authorUuidKey, PersistentDataType.STRING)) {
                        String authorUuid = pdc.get(plugin.authorUuidKey, PersistentDataType.STRING);
                        if (!player.getUniqueId().toString().equals(authorUuid)) {
                            event.setCancelled(true);

                            String authorName = pdc.getOrDefault(plugin.authorNameKey, PersistentDataType.STRING, "Unknown");
                            String notYoursMsg = plugin.getConfig().getString("messages.not-yours", "<red>Hands off! This item was sealed by <author>!</red>")
                                    .replace("<author>", authorName);

                            player.sendActionBar(MiniMessage.miniMessage().deserialize(notYoursMsg));
                            player.playSound(player.getLocation(), getSound("error", Sound.ENTITY_VILLAGER_NO), 1.0f, 1.0f);
                            return;
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;

        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    PersistentDataContainer pdc = meta.getPersistentDataContainer();
                    if (pdc.has(plugin.authorUuidKey, PersistentDataType.STRING)) {
                        String authorUuid = pdc.get(plugin.authorUuidKey, PersistentDataType.STRING);
                        if (!player.getUniqueId().toString().equals(authorUuid)) {
                            event.getInventory().setResult(null);
                            return;
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (plugin.getConfig().getBoolean("recipe.enabled", true)) {
            event.getPlayer().discoverRecipe(new NamespacedKey(plugin, "owner_seal_recipe"));
        }
    }

    @EventHandler
    public void onCrafterCraft(CrafterCraftEvent event) {
        if (event.getBlock().getState() instanceof Crafter crafter) {
            for (ItemStack item : crafter.getInventory().getContents()) {
                if (item != null && item.hasItemMeta()) {
                    PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
                    if (pdc.has(plugin.authorUuidKey, PersistentDataType.STRING)) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }
}

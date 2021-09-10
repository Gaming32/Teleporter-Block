package com.jemnetworks.teleporterblock;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class TeleporterBlockPlugin extends JavaPlugin implements Listener {
    public static final int MODEL_ID = 14555504;
    
    public static NamespacedKey CRAFTING_KEY;
    public static ItemStack TELEPORTER;
    public static File DATA_FILE;

    public Map<Location, TeleporterBlockInfo> blocks;
    public Map<String, TeleporterInfo> teleporters;
    protected boolean dataIsDirty;

    Logger logger;

    @Override
    public void onEnable() {
        logger = getLogger();

        CRAFTING_KEY = new NamespacedKey(this, "teleporter_craft");
        getServer().addRecipe(createRecipie());
        getServer().getPluginManager().registerEvents(this, this);

        loadData();

        logger.info(this + " enabled!");
    }

    @Override
    public void onDisable() {
        saveData();
        logger.info(this + " disabled");
    }

    protected void loadData() {
        if (DATA_FILE == null) {
            DATA_FILE = new File(this.getDataFolder(), "data.yml");
        }
        if (!DATA_FILE.exists()) {
            DATA_FILE.getParentFile().mkdirs();
            saveResource("data.yml", false);
        }

        YamlConfiguration data = new YamlConfiguration();
        try {
            data.load(DATA_FILE);
        } catch (IOException | InvalidConfigurationException e) {
            logger.severe("Failed to load teleporter data. Please correct this issue, then re-enable the plugin.");
            logger.severe(e.toString());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        blocks = new HashMap<>();
        teleporters = new HashMap<>();
        dataIsDirty = false;

        ConfigurationSection teleportersSection = data.getConfigurationSection("teleporters");
        for (String teleporterName : teleportersSection.getKeys(false)) {
            ConfigurationSection teleporterInfo = teleportersSection.getConfigurationSection(teleporterName);
            Location location;
            try {
                location = Location.deserialize(teleporterInfo.getConfigurationSection("location").getValues(false));
            } catch (IllegalArgumentException | ClassCastException e) {
                logger.warning("Failed to load location");
                logger.warning(e.toString());
                continue;
            }
            TeleporterInfo teleporter = new TeleporterInfo(
                teleporterName,
                location,
                teleporterInfo.getString("destination", null),
                teleporters
            );
            teleporters.put(teleporterName, teleporter);
        }

        ConfigurationSection blocksSection = data.getConfigurationSection("blocks");
        for (String blockId : blocksSection.getKeys(false)) {
            ConfigurationSection blockInfo = blocksSection.getConfigurationSection(blockId);
            String teleporterName = blockInfo.getString("teleporter");
            TeleporterInfo teleporter = teleporters.get(teleporterName);
            if (teleporter == null) {
                logger.warning("Teleporter \"" + teleporterName + "\" does not exist");
                continue;
            }
            Location location;
            try {
                location = Location.deserialize(blockInfo.getConfigurationSection("location").getValues(false));
            } catch (IllegalArgumentException | ClassCastException e) {
                logger.warning("Failed to load location");
                logger.warning(e.toString());
                continue;
            }
            String displayEntityUUID = blockInfo.getString("displayEntity");
            UUID displayEntity;
            try {
                displayEntity = UUID.fromString(displayEntityUUID);
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid entity UUID: " + displayEntityUUID);
                logger.warning(e.toString());
                continue;
            }
            TeleporterBlockInfo block = new TeleporterBlockInfo(location, teleporter, displayEntity);
            blocks.put(block.getLocation(), block);
        }

        for (TeleporterInfo teleporter : teleporters.values()) {
            teleporter.calculateRefs(blocks.values().iterator());
        }

        logger.info("Loaded teleporter data");
    }

    protected void saveData() {
        if (DATA_FILE == null) {
            DATA_FILE = new File(this.getDataFolder(), "data.yml");
        }
        if (!DATA_FILE.exists()) {
            DATA_FILE.getParentFile().mkdirs();
        }
        YamlConfiguration data = new YamlConfiguration();
        
        ConfigurationSection teleportersSection = data.createSection("teleporters");
        for (String teleporterName : teleporters.keySet()) {
            TeleporterInfo teleporter = teleporters.get(teleporterName);
            ConfigurationSection teleporterInfo = teleportersSection.createSection(teleporterName);
            teleporterInfo.set("location", teleporter.getLocation().serialize());
            teleporterInfo.set("destination", teleporter.getDestination());
        }

        ConfigurationSection blocksSection = data.createSection("blocks");
        for (Location loc : blocks.keySet()) {
            TeleporterBlockInfo block = blocks.get(loc);
            String blockId = Integer.toString(loc.hashCode());
            ConfigurationSection blockInfo = blocksSection.createSection(blockId);
            blockInfo.set("location", block.getLocation().serialize());
            blockInfo.set("teleporter", block.getTeleporter().getName());
            blockInfo.set("displayEntity", block.getDisplayEntity().toString());
        }

        try {
            data.save(DATA_FILE);
        } catch (IOException e) {
            logger.severe("Failed to save teleporter data.");
            logger.severe(e.toString());
            return;
        }

        logger.info("Saved teleporter data");
    }

    public Location readLocation(ConfigurationSection data) {
        World world = getServer().getWorld(data.getString("world", ""));
        return new Location(world, data.getDouble("x"), data.getDouble("y"), data.getDouble("z"));
    }

    public void writeLocation(ConfigurationSection data, Location loc) {
        data.set("world", loc.getWorld().getName());
        data.set("x", loc.getX());
        data.set("y", loc.getY());
        data.set("z", loc.getZ());
    }

    public boolean isTeleporterItem(ItemStack item) {
        return item.getType() == Material.ITEM_FRAME && item.getItemMeta().getLocalizedName().equals("teleporter_block");
    }

    private ShapedRecipe createRecipie() {
        if (TELEPORTER == null) {
            TELEPORTER = new ItemStack(Material.ITEM_FRAME);
            ItemMeta meta = TELEPORTER.getItemMeta();
            meta.setDisplayName("Teleporter Block");
            meta.setLocalizedName("teleporter_block");
            meta.setCustomModelData(MODEL_ID);
            TELEPORTER.setItemMeta(meta);
        }

        ShapedRecipe recipe = new ShapedRecipe(CRAFTING_KEY, TELEPORTER);
        recipe.shape(" D ", "DSD", "OOO");
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('S', Material.NETHER_STAR);
        recipe.setIngredient('O', Material.OBSIDIAN);

        return recipe;
    }

    @EventHandler
    public void onHangingPlace(HangingPlaceEvent e) {
        if (e.isCancelled()) return;
        ItemStack itemStack = e.getItemStack();
        if (!isTeleporterItem(itemStack)) return;

        e.setCancelled(true);
        itemStack.setAmount(itemStack.getAmount() - 1);

        Block block = e.getBlock().getRelative(e.getBlockFace());
        World world = block.getWorld();
        Location at = block.getLocation();
        block.setType(Material.OBSIDIAN);
        world.playSound(at, Sound.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 1f, 1f);
        
        ArmorStand displayEntity = world.spawn(at, ArmorStand.class, (entity) -> {
            entity.setSmall(true);
            entity.setMarker(true);
            entity.setInvisible(true);
            entity.getEquipment().setHelmet(TELEPORTER, true);
        });

        // System.out.println("Teleporter placed at: " + at);

        String teleporterName = e.getPlayer().getName();
        TeleporterInfo teleporterInfo = teleporters.get(teleporterName);
        if (teleporterInfo == null) {
            teleporterInfo = new TeleporterInfo(teleporterName, at, null);
            teleporters.put(teleporterName, teleporterInfo);
        }
        TeleporterBlockInfo blockInfo = new TeleporterBlockInfo(at, teleporterInfo, displayEntity.getUniqueId());
        blocks.put(at, blockInfo);
        markDirty();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.isCancelled()) return;
        Block block = e.getBlock();
        World world = block.getWorld();
        Location at = block.getLocation();
        if (!(e.getBlock().getType() == Material.OBSIDIAN && blocks.containsKey(at))) return;
        e.setCancelled(true);
        
        if (!block.getDrops(e.getPlayer().getInventory().getItemInMainHand()).isEmpty()) {
            world.dropItemNaturally(at, TELEPORTER);
        }

        block.setType(Material.AIR);
        world.playSound(at, Sound.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, 1f, 1f);

        TeleporterBlockInfo blockInfo = blocks.remove(at);

        Entity displayEntity = getServer().getEntity(blockInfo.getDisplayEntity());
        displayEntity.remove();
        if (!(displayEntity instanceof ArmorStand)) {
            logger.warning("displayEntity not an armor stand: " + displayEntity + " (" + blockInfo.getDisplayEntity() + ")");
        }

        TeleporterInfo teleporter = blockInfo.getTeleporter();
        if (teleporter.ensureValid(blocks.values().iterator()) == 0) {
            teleporters.remove(teleporter.getName());
        }
        markDirty();
    }

    public void markDirty() {
        dataIsDirty = true;
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent e) {
        if (dataIsDirty) {
            saveData();
            dataIsDirty = false;
        }
    }
}

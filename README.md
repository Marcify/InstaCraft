![InstaCraft_banner](https://github.com/user-attachments/assets/4e831e85-7e55-4f1c-a983-b2bc388d986f)

InstaCraft is a Minecraft plugin that allows players to instantly craft items with a simple command. Whether you're an admin who wants to speed up the crafting process or a player looking to easily craft items, InstaCraft is here to make your Minecraft experience more convenient and efficient.

## Features

- **Instant Crafting** - Craft any item instantly with `/craft <item> [amount]`
- **Recipe Browser GUI** - Browse all craftable items with a visual interface
- **Custom Recipes** - Define your own recipes in `recipes.yml`
- **Item Aliases** - Create shortcuts like `dsword` for `DIAMOND_SWORD`
- **Favorites System** - Save frequently crafted items for quick access
- **Economy Integration** - Charge players for crafting (requires Vault)
- **XP System** - Reward or charge XP for crafting
- **Crafting Limits** - Restrict how many items players can craft per period
- **Cooldowns** - Set delays between crafts
- **Statistics Tracking** - Track player crafting history
- **Admin Spy** - Monitor player crafting activity
- **World Restrictions** - Enable/disable in specific worlds
- **PlaceholderAPI Support** - Use placeholders in other plugins
- **Fully Customizable** - Messages, GUI, sounds, particles, and more

## Commands

| Command | Description |
|---------|-------------|
| `/craft <item> [amount]` | Instantly craft an item |
| `/craft list [page]` | List all craftable items |
| `/craft browse` | Open the recipe browser GUI |
| `/craft search` | Search items in the GUI |
| `/craft recipe <item>` | View recipe for an item |
| `/craft fav <add\|remove\|list\|craft>` | Manage favorite items |
| `/craft alias <set\|remove\|list>` | Manage item aliases (admin) |
| `/craft stats` | View your crafting statistics |
| `/craft spy` | Toggle admin craft notifications |
| `/craft reload` | Reload all configuration files |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `instacraft.use` | Use the /craft command | op |
| `instacraft.reload` | Reload configs and manage aliases | op |
| `instacraft.cooldown.bypass` | Bypass crafting cooldown | op |
| `instacraft.limits.bypass` | Bypass crafting limits | op |
| `instacraft.craft.*` | Craft all items | op |
| `instacraft.craft.<item>` | Craft a specific item | op |
| `instacraft.spy` | Toggle admin craft spy | op |

## Configuration Files

- `config.yml` - Main plugin settings
- `messages.yml` - All plugin messages (supports hex colors)
- `gui.yml` - GUI customization
- `recipes.yml` - Custom recipe definitions
- `aliases.yml` - Item alias mappings

## PlaceholderAPI Placeholders

| Placeholder | Description |
|-------------|-------------|
| `%instacraft_total_crafts%` | Player's total craft count |
| `%instacraft_top_item%` | Player's most crafted item |
| `%instacraft_craftable_count%` | Total craftable items |
| `%instacraft_favorites_count%` | Player's saved favorites count |
| `%instacraft_item_<MATERIAL>%` | Craft count for specific item |
| `%instacraft_limit_<MATERIAL>%` | Remaining limit for item |

## Dependencies

- **Required:** Spigot/Paper 1.16+
- **Optional:** Vault (for economy), PlaceholderAPI (for placeholders)

## Support

Contact me on Discord **marcifyx** for assistance or feature suggestions.

**Found a Bug?** Submit an issue [HERE](https://github.com/SandBytes/InstaCraft/issues) and I'll fix it as soon as possible.

[**DOWNLOAD THE PLUGIN**](https://www.spigotmc.org/resources/instacraft.121765/)

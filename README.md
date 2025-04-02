# RegionRegen

A Minecraft Spigot plugin that allows server administrators to create, manage, and regenerate regions through WorldEdit integration and an admin GUI.

![Version](https://img.shields.io/badge/version-1.0.0-green.svg)
![Minecraft](https://img.shields.io/badge/minecraft-1.21%2B-blue.svg)
![License](https://img.shields.io/badge/license-MIT-yellow.svg)

## Features

- Create regions using WorldEdit selections
- Manage regions through both commands and a user-friendly GUI
- Regenerate regions on demand to restore them to their original state
- Track regeneration history for each region
- Automatic backup system for region data
- Teleport to regions directly from the GUI
- Notify nearby players when a region is being regenerated

## Requirements

- Minecraft server running Spigot or Paper 1.16.5 or higher
- WorldEdit plugin (version 7.2.0 or higher)
- Java 17 or higher

## Installation

1. Download the latest release JAR file from the Releases section
2. Place the JAR file in your server's `plugins` folder
3. Restart your server
4. Ensure WorldEdit is installed and working properly

## Usage

### Commands

- `/regionregen` - Open the region management GUI
- `/regionregen create <id>` - Create a new region from your WorldEdit selection
- `/regionregen delete <id>` - Delete a region
- `/regionregen info <id>` - Show information about a region
- `/regionregen list` - List all regions
- `/regionregen regenerate <id>` - Regenerate a region
- `/regionregen help` - Show help information

### Creating a Region

1. Make a WorldEdit selection using the WorldEdit wand or commands (//pos1, //pos2)
2. Use `/regionregen create <id>` with a unique identifier for your region
3. The region will be created and can be managed through the GUI or commands

### Using the GUI

Access the GUI by simply typing `/regionregen` with no arguments. From the GUI, you can:

- View all regions with status indicators
- Create new regions
- View detailed information about regions
- Regenerate regions with a single click
- Delete regions
- Teleport to regions

## Configuration

The plugin's configuration can be found in `plugins/RegionRegen/config.yml`. The main settings include:

```yaml
# Region regeneration settings
regeneration:
  block-delay: 1
  blocks-per-tick: 50
  notify-nearby-players: true
  notification-radius: 100

# Storage settings
storage:
  save-on-modify: true
  auto-backup: true
  backup-interval: 60  # minutes
```

## Permissions

- `regionregen.admin` - Allows access to all RegionRegen commands and features

## Building from Source

1. Clone the repository
2. Build using Gradle: `./gradlew build`
3. The compiled JAR file will be in `build/libs/`

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

Contributions are welcome! Feel free to submit pull requests or open issues for bugs and feature requests.

## Support

If you encounter any issues or have questions, please open an issue on the GitHub repository.
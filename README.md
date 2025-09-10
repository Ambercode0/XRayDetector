
# XRayDetector

![License](https://img.shields.io/badge/License-GPLv3-blue.svg)  
*A PaperMC plugin for advanced X-Ray detection and analysis.*

---

## 📌 Overview

**XRayDetector** is a professional-grade **anti-xray analysis tool** designed for Minecraft servers running [PaperMC](https://papermc.io/).  
Developed by [AmberCode](https://codeberg.org/AmberCode), this plugin provides deep insights into players' mining behaviors to determine if they are **legitimately playing** or exploiting **xray hacks**.

The plugin continuously monitors tunnels, block mining patterns, and ore discoveries to build a **player suspicion score** and determine potential cheaters with unparalleled accuracy.

---

## ✨ Features

- 🔍 **Advanced X-Ray Analysis**  
  Tracks tunnels, blocks dug, ore found, and mining patterns for all players.

- 🖼️ **In-Game GUI Visualizer**  
  Easily view collected player data, including:
  - Suspicion scores  
  - Total ores found  
  - Block mining structures  

- 🌐 **External Web Visualizer**  
  Export and view a **top-down visualization of tunnels** across the server.  
  Perfect for administrators who want a **comprehensive overview**.

- ⚡ **Database Support**  
  - ✅ **SQLite** (currently supported)  
  - 🔜 **MySQL** (coming soon)

- 🛡️ **Ban-Wave System (Planned)**  
  Intelligent banning system with **≥99% accuracy** to ensure fair and reliable punishments.

- 📜 **Open Source**  
  Licensed under **GPLv3**.  
  Official compiled distributions are available **only** on:
  - [SpigotMC](https://www.spigotmc.org/)  
  - [BuiltByBit](https://builtbybit.com/)  
  - [Polymarket](https://polymarket.com/)  

  Any other distributed version is **not legitimate**, and we deny any liability for damages caused by third-party modifications.

---

## 🚀 Installation

1. Download the latest official release from:
   - [SpigotMC](https://www.spigotmc.org/)  
   - [BuiltByBit](https://builtbybit.com/)  
   - [Polymarket](https://polymarket.com/)  

2. Place the `.jar` file into your server’s `plugins/` folder.  
3. Start (or restart) your PaperMC server.  
4. Configure the plugin via the generated `config.yml`.  

---

## 💻 Usage

- **Main Command:**  
```

/xraydetector list

```
Displays a detailed GUI with all collected data.

---

## 🛠️ Tech Stack

- **Platform:** PaperMC  
- **Language:** Java 21  
- **Databases:** SQLite (supported), MySQL (upcoming)  

---

## 🎯 Milestones

- [x] Basic tunnel and ore mining tracking  
- [x] Suspicion score system  
- [x] GUI visualizer in-game  
- [ ] Web visualization tool (top-down tunnel map)  
- [ ] MySQL database support  
- [ ] Intelligent ban-wave system  
- [ ] Performance optimizations for large servers  

---

## 📖 License

This project is licensed under the **GNU General Public License v3.0**.  
See [LICENSE](LICENSE) for details.

---

## 🤝 Contributing

Contributions are welcome!  
Please fork the repository, make your changes, and submit a pull request.

---

## How to Compile

### Requirements

- **Java 21 JDK** or higher installed on your system.  
- **Maven** with a recent version installed on your system.  
- **Git** installed on your system.  

1. Clone the repository in a terminal using `git clone https://codeberg.org/AmberCode/XRayDetector.git`. 
2. Make sure that you have **Maven** installed. 
3. Compile the project by running `mvn clean install` then `mvn clean package`. 

---

## 📢 Disclaimer

Only **compiled JAR files** distributed on **SpigotMC**, **BuiltByBit**, and **Polymarket** are considered **official releases**.  
Any third-party distributed files are **not legitimate** and may cause server issues. We are **not liable** for damages caused by unauthorized versions.

---
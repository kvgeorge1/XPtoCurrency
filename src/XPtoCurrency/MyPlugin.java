package XPtoCurrency;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import joebkt._EconomyManager;
import PluginReference.ChatColor;
import PluginReference.MC_Command;
import PluginReference.MC_DamageType;
import PluginReference.MC_Entity;
import PluginReference.MC_EntityType;
import PluginReference.MC_EventInfo;
import PluginReference.MC_Player;
import PluginReference.MC_Server;
import PluginReference.PluginBase;
import PluginReference.PluginInfo;

public class MyPlugin extends PluginBase {
	private static final String CONFIG_DIR = "plugins_mod" + File.separatorChar + "XPtoCurrency";
	private static File config = new File(CONFIG_DIR + File.separatorChar + "config.ini");

	public static MC_Server server;

	private int _xpValue = 0;
	private int _rate = 1;
	private boolean _doXpGeneration = true;

	@Override
	public PluginInfo getPluginInfo() {
		PluginInfo info = new PluginInfo();
		info.name = "XPtoCurrency";
		info.description = "Converts XP to server currency";
		info.version = "1.0";
		return info;
	}

	@Override
	public void onStartup(final MC_Server server) {
		MyPlugin.server = server;

		// TODO: Read-in config
		//
		if (!config.exists()) {
			// Make properties file
			//
			BufferedWriter writer = null;
			try {
				File dir = new File(CONFIG_DIR);
				dir.mkdirs();

				config.createNewFile();
				writer = new BufferedWriter(new FileWriter(config));
				writer.write("# XPtoCurrency Configuration File\n");
				writer.write("#\n");
				writer.write("rate=100\n");
//				writer.write("xp_orb_generation=true\n");
				writer.flush();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		// Read Properties
		//
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(config));
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#") || line.trim().equals("")) {
					// Skip
				} else {
					String[] parts = line.split("=");
					if (parts[0].equals("rate")) {
						try {
							_rate = Integer.parseInt(parts[1].trim());
						} catch (NumberFormatException nfe) {
							server.log("[XPtoCurrency] Unknown rate: " + parts[1].trim());
						}
					}
//					if (parts[0].equals("xp_orb_generation")) {
//						if (parts[1].trim().toLowerCase().equals("false")) {
//							_doXpGeneration = false;
//						}
//					}
				}
			}
		} catch (FileNotFoundException e) {
			server.log("[XPtoCurrency] Unable to find configuration file: " + config.getPath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Register new command
		//
		server.registerCommand(new MC_Command() {

			@Override
			public List<String> getAliases() {
				return new ArrayList<String>();
			}

			@Override
			public String getCommandName() {
				return "sellxp";
			}

			@Override
			public String getHelpLine(MC_Player plr) {
				return "/sellxp help - type '/sellxp help' for info";
			}

			@Override
			public List<String> getTabCompletionList(MC_Player plr, String[] args) {
				return new ArrayList<String>();
			}

			@Override
			public void handleCommand(MC_Player plr, String[] args) {
				if (plr == null) {
					server.log("You must be a PLAYER!!!");
					return;
				}

				// Sell All
				//
				if (args.length < 1 || args[0].equals("help")) {
					plr.sendMessage(ChatColor.AQUA + "/sellxp all" + ChatColor.WHITE + " - sells ALL of your XP to the server");
					plr.sendMessage(ChatColor.AQUA + "/sellxp <amount>" + ChatColor.WHITE
							+ " - sells the specified amount of XP to the server");
					plr.sendMessage(ChatColor.AQUA + "/sellxp balance" + ChatColor.WHITE
							+ " - tells you the amount of XP you still have remaining");
					plr.sendMessage(ChatColor.AQUA + "/sellxp ratio" + ChatColor.WHITE
							+ " - tells you the current exchange rate in xp:currency (e.g. 100:1)");
				} else if (args[0].equals("all")) {
					int xp = plr.getTotalExperience();

					// Convert using ratio
					//
					int remainingXp = xp % _rate;
					double pay = (xp / _rate);
					_EconomyManager.Deposit(plr.getName(), pay);

					// Calculate remaining XP (if any)
					//
					plr.setTotalExperience(remainingXp);
					plr.setLevel(XPHelper.findLevel(plr.getTotalExperience()));

					plr.sendMessage(ChatColor.GREEN + "Added " + ChatColor.WHITE + pay + ChatColor.GREEN + " to your balance."
							+ ChatColor.AQUA + " New Balance: " + ChatColor.WHITE + _EconomyManager.GetBalance(plr.getName()));
				} else if (args[0].equals("balance") || args[0].equals("bal")) {
					plr.sendMessage("Your current XP balance is: " + ChatColor.GREEN + plr.getTotalExperience() + " ("
							+ XPHelper.findLevel(plr.getTotalExperience()) + " Levels)");
				} else if (args[0].equals("rate")) {
					plr.sendMessage("The current exchange rate of xp:currency is: " + ChatColor.GREEN + _rate + ":1");
				} else {
					try {
						boolean useLevels = false;
						int sellXp = 0;
						if (args[0].endsWith("l") || args[0].endsWith("L")) {
							useLevels = true;
							sellXp = Integer.parseInt(args[0].trim().toLowerCase().replace("l", ""));
						} else {
							sellXp = Integer.parseInt(args[0].trim());
						}

						// Check that user has enough XP/Levels to sell
						//
						if (useLevels && (plr.getLevel() < sellXp)) {
							plr.sendMessage(ChatColor.RED + "You do not have enough levels to sell that much!");
							return;
						} else {
							if (plr.getTotalExperience() < sellXp) {
								plr.sendMessage(ChatColor.RED + "You do not have enough XP to sell that much!");
								return;
							}
						}

						if (sellXp < _rate) {
							plr.sendMessage(ChatColor.RED + "You must sell at least " + _rate + " in order to convert!");
							return;
						}

						// Perform sale
						//
						int xp = plr.getTotalExperience();

						// Convert using ratio
						//
						int remainingXp = sellXp % _rate;
						double pay = (sellXp / _rate);
						_EconomyManager.Deposit(plr.getName(), pay);

						// Calculate remaining XP (if any)
						//
						xp -= sellXp;
						plr.setTotalExperience(xp + remainingXp);
						plr.setLevel(XPHelper.findLevel(plr.getTotalExperience()));

						plr.sendMessage(ChatColor.GREEN + "Added " + ChatColor.WHITE + pay + ChatColor.GREEN + " to your balance."
								+ ChatColor.AQUA + " New Balance: " + ChatColor.WHITE + _EconomyManager.GetBalance(plr.getName()));
					} catch (NumberFormatException nfe) {
						plr.sendMessage(ChatColor.RED + "Invalid argument! Please use /sellxp help for proper use.");
					}
				}
			}

			@Override
			public boolean hasPermissionToUse(MC_Player plr) {
				return true;
			}

		});

		System.out.println("[XPtoCurrency] Activated!");
	}

	@Override
	public void onShutdown() {
		System.out.println("[XPtoCurrency] Deactivated!");
	}

	@Override
	public void onAttemptDeath(MC_Entity entVictim, MC_Entity entKiller, MC_DamageType dmgType, float dmgAmount) {
		// Take over if set to NOT do XP ORB generation
		//
		if (!this._doXpGeneration) {
			if (entVictim.getType() != MC_EntityType.PLAYER && entKiller.getType() == MC_EntityType.PLAYER) {
				MC_Player plr = (MC_Player) entKiller;
				plr.setExp(plr.getExp() + _xpValue);
			}
		} else {
			super.onAttemptDeath(entVictim, entKiller, dmgType, dmgAmount);
		}
	}

	@Override
	public void onAttemptEntitySpawn(MC_Entity ent, MC_EventInfo ei) {
		// Cancel XP ORB generation if configured to NOT generate XP
		//
		if (!_doXpGeneration) {
			if (ent.getType() == MC_EntityType.XP_ORB) {
				ent.removeEntity();
				ei.isCancelled = true;
			}
		}
	}

}

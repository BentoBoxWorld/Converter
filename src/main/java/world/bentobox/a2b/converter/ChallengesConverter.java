package world.bentobox.a2b.converter;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bskyblock.BSkyBlock;
import world.bentobox.challenges.ChallengesAddon;
import world.bentobox.challenges.database.object.Challenge;
import world.bentobox.challenges.database.object.ChallengeLevel;
import world.bentobox.challenges.database.object.requirements.InventoryRequirements;
import world.bentobox.challenges.database.object.requirements.IslandRequirements;
import world.bentobox.challenges.database.object.requirements.OtherRequirements;
import world.bentobox.challenges.utils.GuiUtils;
import world.bentobox.challenges.utils.Utils;


/**
 * This class contains all necessary methods to successfully translate ASkyBlock challenges
 * into BentoBox Challenges addon.
 */
public class ChallengesConverter
{
	/**
	 * This is default constructor for ChallengeConverter.
	 */
	public ChallengesConverter()
	{
		this.addon = BentoBox.getInstance().getAddonsManager().getAddonByName("Challenges").
			map(ChallengesAddon.class::cast).orElse(null);

		if (this.addon == null)
		{
			BentoBox.getInstance().logWarning("Challenges addon not found. Challenges cannot be imported!");
		}
	}


	/**
	 * This method returns if ASkyBlock challenges can be converted.
	 * @return {@code true} if Challenges Addon exists, otherwise {@code false}.
	 */
	public boolean canConvert()
	{
		return this.addon != null;
	}


// ---------------------------------------------------------------------
// Section: Convert ASkyBlock Challenges to BentoBox Challenges Addon
// ---------------------------------------------------------------------


	/**
	 * Import challenges
	 *
	 * @param user - user
	 * @param world - world to import into
	 * @return true if successful
	 */
	public boolean convertChallengesAndLevels(User user,
		World world)
	{
		File challengeFile = new File(this.addon.getPlugin().getDataFolder(),
			"../ASkyBlock/challenges.yml");

		if (!challengeFile.exists())
		{
			BentoBox.getInstance().logWarning("challenges.errors.import-no-file");
			return false;
		}

		this.challengesConfiguration = new YamlConfiguration();

		try
		{
			this.challengesConfiguration.load(challengeFile);
		}
		catch (IOException | InvalidConfigurationException e)
		{
			user.sendMessage("challenges.errors.no-load", "[message]", e.getMessage());
			return false;
		}

		// These maps links old challenges and levels to new objects.
		this.challengeLinkMap = new HashMap<>();
		this.levelLinkMap = new HashMap<>();

		this.makeLevels(user, world);
		this.makeChallenges(user, world);

		this.addon.getChallengesManager().save();

		return true;
	}


	/**
	 * Imports levels
	 */
	private void makeLevels(User user,
		World world)
	{
		// Parse the levels
		String levels = this.challengesConfiguration.getString("challenges.levels", "");

		if (levels != null && !levels.isEmpty())
		{
			user.sendMessage("challenges.messages.import-levels");
			String[] lvs = levels.split(" ");
			int order = 0;

			for (String level : lvs)
			{
				ChallengeLevel challengeLevel = new ChallengeLevel();
				challengeLevel.setFriendlyName(level);
				challengeLevel.setUniqueId(Utils.getGameMode(world) + "_" + level);
				challengeLevel.setOrder(order++);
				challengeLevel.setWorld(Util.getWorld(world).getName());
				challengeLevel.setWaiverAmount(this.challengesConfiguration.getInt("challenges.waiveramount"));
				// Check if there is a level reward
				ConfigurationSection unlock = this.challengesConfiguration.getConfigurationSection("challenges.levelUnlock." + level);

				if (unlock != null)
				{
					challengeLevel.setUnlockMessage(unlock.getString("message", ""));
					challengeLevel.setRewardText(unlock.getString("rewardDesc", ""));
					challengeLevel.setRewardItems(this.parseItems(Objects.requireNonNull(unlock.getString("itemReward", ""))));
					challengeLevel.setRewardMoney(unlock.getInt("moneyReward"));
					challengeLevel.setRewardExperience(unlock.getInt("expReward"));
					challengeLevel.setRewardCommands(unlock.getStringList("commands"));
				}

				this.addon.getChallengesManager().loadLevel(challengeLevel, false, user, false);
				this.levelLinkMap.put(level, challengeLevel);
			}
		}
		else
		{
			user.sendMessage("challenges.messages.no-levels");
		}
	}


	/**
	 * Imports challenges
	 */
	private void makeChallenges(User user,
		World world)
	{
		int size = 0;
		// Parse the challenge file
		ConfigurationSection challengesSection = this.challengesConfiguration.getConfigurationSection("challenges.challengeList");
		user.sendMessage("challenges.messages.import-challenges");

		for (String challenge : Objects.requireNonNull(challengesSection).getKeys(false))
		{
			Challenge newChallenge = new Challenge();
			newChallenge.setUniqueId(Utils.getGameMode(world) + "_" + challenge);
			newChallenge.setDeployed(true);
			ConfigurationSection details = challengesSection.getConfigurationSection(challenge);
			newChallenge.setFriendlyName(Objects.requireNonNull(details).getString("friendlyname", challenge));

			// Split description lines
			newChallenge.setDescription(
				GuiUtils.stringSplit(details.getString("description", ""),
					this.addon.getChallengesSettings().getLoreLineLength()));

			newChallenge.setIcon(ItemDataParserUtil.parseItem(details.getString("icon", "") + ":1"));

			if (Objects.requireNonNull(details.getString("type", "")).equalsIgnoreCase("level"))
			{
				// Fix for older version config
				newChallenge.setChallengeType(Challenge.ChallengeType.OTHER);
			}
			else
			{
				newChallenge.setChallengeType(Challenge.ChallengeType.valueOf(
					Objects.requireNonNull(details.getString("type", "INVENTORY")).toUpperCase()));
			}

			newChallenge.setRewardText(details.getString("rewardText", ""));
			newChallenge.setRewardCommands(details.getStringList("rewardcommands"));
			newChallenge.setRewardMoney(details.getInt("moneyReward", 0));
			newChallenge.setRewardExperience(details.getInt("expReward"));
			newChallenge.setRepeatable(details.getBoolean("repeatable"));
			newChallenge.setRepeatRewardText(details.getString("repeatRewardText", ""));
			newChallenge.setRepeatMoneyReward(details.getInt("repearMoneyReward"));
			newChallenge.setRepeatExperienceReward(details.getInt("repeatExpReward"));
			newChallenge.setRepeatRewardCommands(details.getStringList("repeatrewardcommands"));
			newChallenge.setMaxTimes(details.getInt("maxtimes"));
			
			String reqItems = details.getString("requiredItems", "");

			if (newChallenge.getChallengeType().equals(Challenge.ChallengeType.INVENTORY))
			{
				InventoryRequirements requirements = new InventoryRequirements();

				requirements.setRequiredItems(this.parseItems(Objects.requireNonNull(reqItems)));
				requirements.setTakeItems(details.getBoolean("takeItems", true));

				newChallenge.setRequirements(requirements);
			}
			else if (newChallenge.getChallengeType().equals(Challenge.ChallengeType.OTHER))
			{
				OtherRequirements requirements = new OtherRequirements();

				requirements.setRequiredIslandLevel(Long.parseLong(Objects.requireNonNull(reqItems)));
				requirements.setRequiredMoney(details.getInt("requiredMoney"));
				requirements.setRequiredExperience(details.getInt("requiredExp"));

				requirements.setTakeMoney(details.getBoolean("takeItems", true));
				requirements.setTakeExperience(details.getBoolean("takeItems", true));

				newChallenge.setRequirements(requirements);
			}
			else if (newChallenge.getChallengeType().equals(Challenge.ChallengeType.ISLAND))
			{
				IslandRequirements requirements = new IslandRequirements();
				this.parseEntitiesAndBlocks(requirements, Objects.requireNonNull(reqItems));

				newChallenge.setRequirements(requirements);
			}

			newChallenge.setRewardItems(parseItems(details.getString("itemReward", "")));
			newChallenge.setRepeatItemReward(parseItems(details.getString("repeatItemReward", "")));

			this.addon.getChallengesManager().addChallengeToLevel(newChallenge,
				this.addon.getChallengesManager().getLevel(Utils.getGameMode(world) + "_" + details.getString("level", "")));

			if (this.addon.getChallengesManager().loadChallenge(newChallenge, false, user, false))
			{
				size++;
			}

			this.challengeLinkMap.put(challenge, newChallenge);
		}

		user.sendMessage("challenges.messages.import-number",
			"[number]",
			String.valueOf(size));
	}


	/**
	 * Run through entity types and materials and try to match to the string given
	 *
	 * @param requirements - requirements to be adjusted
	 * @param string - string from YAML file
	 */
	private void parseEntitiesAndBlocks(IslandRequirements requirements, String string)
	{
		Map<EntityType, Integer> requiredEntities = new EnumMap<>(EntityType.class);
		Map<Material, Integer> requiredBlocks = new EnumMap<>(Material.class);

		if (!string.isEmpty())
		{
			for (String singleItem : string.split(" "))
			{
				String[] parts = singleItem.split(":");

				try
				{
					EntityType type = ItemDataParserUtil.parseEntity(parts[0]);

					if (type != null)
					{
						requiredEntities.put(type, Integer.parseInt(parts[1]));
					}
					else
					{
						Material material = ItemDataParserUtil.parseMaterial(parts[0], true);

						if (material != null)
						{
							requiredBlocks.put(material, Integer.parseInt(parts[1]));
						}
						else
						{
							BentoBox.getInstance().logError("Could not parse as Entity or Block '" + singleItem + "'.");
						}
					}
				}
				catch (Exception e)
				{
					BentoBox.getInstance().getLogger().severe("Cannot parse '" + singleItem + "'. Skipping...");
				}
			}
		}

		requirements.setRequiredEntities(requiredEntities);
		requirements.setRequiredBlocks(requiredBlocks);
	}


	/**
	 * This method converts input string that contains multiple items as strings into
	 * coorect ItemStacks.
	 * @param inputString InputString that contains multiple items separated with white space;
	 * @return List of ItemStacks that contains translated inputString items.
	 */
	private List<ItemStack> parseItems(String inputString)
	{
		List<ItemStack> result = new ArrayList<>();

		if (!inputString.isEmpty())
		{
			for (String singleItem : inputString.split(" "))
			{
				ItemStack itemStack = ItemDataParserUtil.parseItem(singleItem);

				if (itemStack != null)
				{
					result.add(itemStack);
				}
			}
		}

		return result;
	}


// ---------------------------------------------------------------------
// Section: Convert ASkyBlock Player Data Challenges to BentoBox Challenges Addon
// ---------------------------------------------------------------------


	/**
	 * This method converts ASkyBlock challenges User Data to new BentoBox Challenges User data.
	 * @param user User which data must be converted.
	 * @param playerData User data file.
	 * @param bSkyBlock BSkyBlockAddon instance (can be replaced with world).
	 */
	public void convertUserChallenges(User user, YamlConfiguration playerData, BSkyBlock bSkyBlock)
	{
		this.challengeLinkMap.forEach((challengeName, challengeObject) -> {

			if (playerData.getBoolean("challenges.status." + challengeName.toLowerCase().replace(".", "[dot]"), false))
			{
				int completionCount = playerData.getInt("challenges.times." + challengeName.toLowerCase().replace(".", "[dot]"), 0);

				if (completionCount > 0)
				{
					this.addon.getChallengesManager().setChallengeComplete(user,
						bSkyBlock.getOverWorld(),
						challengeObject,
						completionCount);
				}

				// TODO: currently timestamp cannot be passed to new challenges.
				long timeStamp = playerData.getLong("challenges.timestamp." + challengeName.toLowerCase().replace(".", "[dot]"), 0);
			}
		});

		this.levelLinkMap.forEach((levelName, levelObject) -> {

			if (playerData.getBoolean("challenges.status." + levelName.toLowerCase().replace(".", "[dot]"), false))
			{
				int completionCount = playerData.getInt("challenges.times." + levelName.toLowerCase().replace(".", "[dot]"), 0);

				if (completionCount > 0)
				{
					this.addon.getChallengesManager().setLevelComplete(user,
						bSkyBlock.getOverWorld(),
						levelObject);
				}

				// TODO: currently timestamp cannot be passed to new levels.
				long timeStamp = playerData.getLong("challenges.timestamp." + levelName.toLowerCase().replace(".", "[dot]"), 0);
			}
		});
	}


// ---------------------------------------------------------------------
// Section: Instance Variables
// ---------------------------------------------------------------------


	/**
	 * This map links Old challenges and levels to new Challenges and levels.
	 */
	private Map<String, Challenge> challengeLinkMap;

	private Map<String, ChallengeLevel> levelLinkMap;

	/**
	 *  Field challengesConfiguration
	 */
	private YamlConfiguration challengesConfiguration;

	/**
	 * This field allows to access to challenges addon object.
	 */
	private ChallengesAddon addon;
}

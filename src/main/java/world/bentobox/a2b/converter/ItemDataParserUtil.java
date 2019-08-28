package world.bentobox.a2b.converter;


import org.apache.commons.lang.StringUtils;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import de.themoep.idconverter.EntitiesIdMappings;
import de.themoep.idconverter.IdMappings;
import world.bentobox.bentobox.BentoBox;


public class ItemDataParserUtil
{
// ---------------------------------------------------------------------
// Section: Public Methods
// ---------------------------------------------------------------------


	/**
	 * This method parses given input string into ItemStack.
	 * @param inputString Input String that must be parsed to ItemStack.
	 * @return ItemStack that corresponds inputString.
	 */
	public static ItemStack parseItem(String inputString)
	{
		// Used to know how many parts for item exists.
		// Assumption -> every string ends with :X where X is a integer that is larger
		// than 0 and represents number of items in stack.

		String[] inputArray = inputString.split(":");

		// Validate if assumption is correct
		if (!StringUtils.isNumeric(inputArray[inputArray.length - 1]))
		{
			BentoBox.getInstance().logError("Could not parse itemString to item. Item count is not a valid Integer: " + inputString);
			return null;
		}

		// Single check on numeric value
		final boolean isNumeric = StringUtils.isNumeric(inputArray[0]);

		if (ItemDataParserUtil.MONSTER_EGG.equalsIgnoreCase(inputArray[0]) ||
			isNumeric && ItemDataParserUtil.MONSTER_EGG_ID == Integer.parseInt(inputArray[0]))
		{
			// Process monster egg.
			return ItemDataParserUtil.parseMonsterEggItemStack(inputString);
		}
		else if (ItemDataParserUtil.TIPPED_ARROW.equalsIgnoreCase(inputArray[0]) ||
			ItemDataParserUtil.POTION.equalsIgnoreCase(inputArray[0]) ||
			isNumeric && ItemDataParserUtil.POTION_ID == Integer.parseInt(inputArray[0]))
		{
			// Process potions
			return ItemDataParserUtil.parsePotion(inputString);
		}
		else if (ItemDataParserUtil.BANNER.equalsIgnoreCase(inputArray[0]) ||
			isNumeric && ItemDataParserUtil.BANNER_ID == Integer.parseInt(inputArray[0]))
		{
			// Process banners
			return ItemDataParserUtil.parseBanner(inputString);
		}
		else
		{
			// This string will be used to get correct item, by removing stack count.
			String parsingString = inputArray.length == 2 ?
				inputArray[0] :
				inputArray[0] + ":" + inputArray[1];

			IdMappings.Mapping objectMapping = IdMappings.get(
				isNumeric ? IdMappings.IdType.NUMERIC : IdMappings.IdType.LEGACY,
				parsingString);

			if (objectMapping == null)
			{
				BentoBox.getInstance().logError("Could not parse itemString to item. Item not found in legacy code: " + inputString);
				return null;
			}

			Material itemMaterial = Material.getMaterial(objectMapping.getFlatteningType());

			if (itemMaterial == null)
			{
				BentoBox.getInstance().logError("Could not parse itemString to item. Item not found in Material List: " + inputString);
				return null;
			}

			// Return new ItemStack with correct material and count
			return new ItemStack(itemMaterial,
				Integer.parseInt(inputArray[inputArray.length - 1]));
		}
	}


	/**
	 * This method parses given input string into Material.
	 * @param inputString Input String that must be parsed to Material.
	 * @return Material that corresponds inputString.
	 */
	public static Material parseMaterial(String inputString)
	{
		return ItemDataParserUtil.parseMaterial(inputString, false);
	}


	/**
	 * This method parses given input string into Material.
	 * @param inputString Input String that must be parsed to Material.
	 * @param silent report an error in console.
	 * @return Material that corresponds inputString.
	 */
	public static Material parseMaterial(String inputString, boolean silent)
	{
		// Used to know how many parts for item exists.
		// Assumption -> every string ends with :X where X is a integer that is larger
		// than 0 and represents number of items in stack.

		String[] inputArray = inputString.split(":");

		// Single check on numeric value
		final boolean isNumeric = StringUtils.isNumeric(inputArray[0]);

		if (ItemDataParserUtil.MONSTER_EGG.equalsIgnoreCase(inputArray[0]) ||
			isNumeric && ItemDataParserUtil.MONSTER_EGG_ID == Integer.parseInt(inputArray[0]))
		{
			// Process monster egg.
			return ItemDataParserUtil.parseMonsterEggMaterial(inputString);
		}
		else
		{
			// This string will be used to get correct item, by removing stack count.
			String parsingString = inputArray.length == 1 ?
				inputArray[0] :
				inputArray[0] + ":" + inputArray[1];

			IdMappings.Mapping objectMapping = IdMappings.get(
				isNumeric ? IdMappings.IdType.NUMERIC : IdMappings.IdType.LEGACY,
				parsingString);

			if (objectMapping == null)
			{
				if (!silent)
				{
					BentoBox.getInstance().logError("Could not parse itemString to Material. Item not found in legacy code: " + inputString);
				}

				return null;
			}

			Material itemMaterial = Material.getMaterial(objectMapping.getFlatteningType());

			if (itemMaterial == null)
			{
				if (!silent)
				{
					BentoBox.getInstance().logError("Could not parse itemString to Material. Item not found in Material List: " + inputString);
				}

				return null;
			}

			// Return new ItemStack with correct material and count
			return itemMaterial;
		}
	}


	/**
	 * This method parses input string to Entity which it corresponds.
	 * @param inputString Input String that must be converted to Entity.
	 * @return EntityType that represents current string.
	 */
	public static EntityType parseEntity(String inputString)
	{
		String[] inputArray = inputString.split(":");

		return ENTITY_MAP.get(inputArray[0].toUpperCase());
	}


// ---------------------------------------------------------------------
// Section: Private Methods
// ---------------------------------------------------------------------


	/**
	 * This method parses Banner to ItemStack.
	 * @param inputString InputString that must be parsed.
	 * @return Baner ItemStack item.
	 */
	private static ItemStack parseBanner(String inputString)
	{
		String[] inputArray = inputString.split(":");

		if (inputArray.length < 2)
		{
			BentoBox.getInstance().logError("Could not parse itemString to Banner. Banner parser need at least 2 elements: " + inputString);
			return null;
		}

		Material bannerMaterial = Material.getMaterial(inputArray[0]);

		if (bannerMaterial == null)
		{
			BentoBox.getInstance().logError("Could not parse banner item " + inputArray[0] + " so using a white banner.");
			bannerMaterial = Material.WHITE_BANNER;
		}

		ItemStack result = new ItemStack(bannerMaterial, Integer.parseInt(inputArray[1]));
		BannerMeta meta = (BannerMeta) result.getItemMeta();

		if (meta != null)
		{
			for (int i = 2; i < inputArray.length; i += 2)
			{
				meta.addPattern(new Pattern(DyeColor.valueOf(inputArray[i + 1]), PatternType.valueOf(inputArray[i])));
			}

			result.setItemMeta(meta);
		}

		return result;
	}


	/**
	 * This method parses Potions (defined as ASkyBlock challenges file).
	 * @param inputString InputString that must be parsed to potion.
	 * @return Parsed potion ItemStack.
	 */
	private static ItemStack parsePotion(String inputString)
	{
		String[] inputArray = inputString.split(":");

		if (inputArray.length != 6)
		{
			BentoBox.getInstance().logError("Could not parse itemString to Potion. Potion parser need 6 elements: " + inputString);
			return null;
		}

		ItemStack itemStack;

		if (TIPPED_ARROW.equalsIgnoreCase(inputArray[0]))
		{
			itemStack = new ItemStack(Material.TIPPED_ARROW,
				Integer.parseInt(inputArray[5]));
		}
		else if (SPLASH.equalsIgnoreCase(inputArray[4]))
		{
			itemStack = new ItemStack(Material.SPLASH_POTION,
				Integer.parseInt(inputArray[5]));
		}
		else if (LINGER.equalsIgnoreCase(inputArray[4]))
		{
			itemStack = new ItemStack(Material.LINGERING_POTION,
				Integer.parseInt(inputArray[5]));
		}
		else
		{
			itemStack = new ItemStack(Material.POTION,
				Integer.parseInt(inputArray[5]));
		}

		PotionMeta potionMeta = (PotionMeta) itemStack.getItemMeta();
		PotionType type = PotionType.valueOf(inputArray[1].toUpperCase(Locale.ENGLISH));
		boolean isUpgraded = !inputArray[2].isEmpty() && !inputArray[2].equalsIgnoreCase("1");
		boolean isExtended = EXTENDED.equalsIgnoreCase(inputArray[3]);
		PotionData data = new PotionData(type, isExtended, isUpgraded);
		Objects.requireNonNull(potionMeta).setBasePotionData(data);

		return itemStack;
	}


	/**
	 * This method parses given input string to ItemStack that contains MonsterEgg.
	 * @param inputString InputString that must be parsed to MonsterEgg.
	 * @return ItemStack that contains correct MonsterEgg.
	 */
	private static ItemStack parseMonsterEggItemStack(String inputString)
	{
		String[] inputArray = inputString.split(":");

		if (inputArray.length != 3)
		{
			BentoBox.getInstance().logError("Could not parse itemString to MonsterEgg. MonsterEgg parser need 3 elements: " + inputString);
			return null;
		}

		Material material = ItemDataParserUtil.parseMonsterEggMaterial(inputArray[0] + ":" + inputArray[1]);

		if (material == null)
		{
			return null;
		}

		return new ItemStack(material, Integer.parseInt(inputArray[2]));
	}


	/**
	 * This method parses given input string to Material that contains MonsterEgg.
	 * @param inputString Material that must be parsed to MonsterEgg.
	 * @return Material that contains correct MonsterEgg.
	 */
	private static Material parseMonsterEggMaterial(String inputString)
	{
		String[] inputArray = inputString.split(":");

		if (inputArray.length != 2)
		{
			BentoBox.getInstance().logError("Could not parse itemString to MonsterEgg. MonsterEgg parser need 2 elements: " + inputString);
			return null;
		}

		String entityId;

		if (!StringUtils.isNumeric(inputArray[1]))
		{
			EntitiesIdMappings.Mapping mapping = EntitiesIdMappings.get(EntitiesIdMappings.IdType.LEGACY, inputArray[1]);

			if (mapping == null)
			{
				BentoBox.getInstance().logError("Could not parse itemString to MonsterEgg. Legacy name for Entity not found: " + inputString);
				return null;
			}

			entityId = mapping.getNumericId() + "";
		}
		else
		{
			entityId = inputArray[1];
		}


		IdMappings.Mapping mapping = IdMappings.get(IdMappings.IdType.NUMERIC, MONSTER_EGG_ID + ":" + entityId);

		if (mapping == null)
		{
			BentoBox.getInstance().logError("Could not parse itemString to MonsterEgg. Legacy name for MonsterEgg not found: " + inputString);
			return null;
		}

		Material material = Material.getMaterial(mapping.getFlatteningType());

		if (material == null)
		{
			BentoBox.getInstance().logError("Could not parse itemString to MonsterEgg. Material for MonsterEgg not found: " + inputString);
			return null;
		}

		return material;
	}


// ---------------------------------------------------------------------
// Section: Entities
// ---------------------------------------------------------------------

	/**
	 * This map contains all entities that exist in current Minecraft version.
	 */
	private static final Map<String, EntityType> ENTITY_MAP = new HashMap<>();

	/**
	 * Populate map with all entities.
	 */
	static
	{
		for (EntityType value : EntityType.values())
		{
			ENTITY_MAP.put(value.name(), value);
		}
	}

// ---------------------------------------------------------------------
// Section: Constants
// ---------------------------------------------------------------------


	private static final String POTION = "POTION";

	private static final int POTION_ID = 373;

	private static final String TIPPED_ARROW = "TIPPED_ARROW";

	private static final String SPLASH = "SPLASH";

	private static final String LINGER = "LINGER";

	private static final String EXTENDED = "EXTENDED";

	private static final String MONSTER_EGG = "MONSTER_EGG";

	private static final int MONSTER_EGG_ID = 383;

	private static final String BANNER = "BANNER";

	private static final int BANNER_ID = 425;
}

package dev.alist.progressivedifficulty;

import dev.alist.progressivedifficulty.commands.DifficultyCommand;
import dev.alist.progressivedifficulty.behaviour.HostileBehaviourManager;
import dev.alist.progressivedifficulty.config.ConfigManager;
import dev.alist.progressivedifficulty.difficulty.AttributeModifierManager;
import dev.alist.progressivedifficulty.difficulty.EquipmentManager;
import dev.alist.progressivedifficulty.passive.PassiveRetaliationManager;
import dev.alist.progressivedifficulty.progression.MilestoneTracker;
import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProgressiveDifficulty implements ModInitializer {
	public static final String MOD_ID = "progressivedifficulty";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ConfigManager.initialize();
		MilestoneTracker.register();
		AttributeModifierManager.register();
		EquipmentManager.register();
		HostileBehaviourManager.register();
		PassiveRetaliationManager.register();
		DifficultyCommand.register();
		LOGGER.info("Progressive Difficulty initialized.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}

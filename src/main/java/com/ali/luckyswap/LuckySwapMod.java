package com.ali.luckyswap;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class LuckySwapMod implements ModInitializer {

	// ===== الإعدادات =====
	private static final int SWAP_TICKS = 20 * 60 * 10; // كل 10 دقايق
	private static final int GIVE_TICKS = 20 * 10;      // كل 10 ثواني
	// ======================

	private static final Random RNG = new Random();
	private static int swapTimer = SWAP_TICKS;
	private static int giveTimer = GIVE_TICKS;

	@Override
	public void onInitialize() {
		ServerTickEvents.END_SERVER_TICK.register(LuckySwapMod::onTick);

		// كسر اللكي بلوك
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
			if (world instanceof ServerLevel level
					&& player instanceof ServerPlayer sp
					&& state.is(Blocks.YELLOW_GLAZED_TERRACOTTA)) {
				level.removeBlock(pos, false);
				openLucky(level, pos, sp);
				return false; // نلغي الكسر العادي حتى ما ينزل البلوك نفسه
			}
			return true;
		});
	}

	// ---------------- التايمر ----------------

	private static void onTick(MinecraftServer server) {
		List<ServerPlayer> players = server.getPlayerList().getPlayers();
		if (players.isEmpty()) return; // يوقف التايمر إذا ما في لاعبين

		if (--giveTimer <= 0) {
			giveTimer = GIVE_TICKS;
			for (ServerPlayer p : players) giveLuckyBlock(p);
		}

		swapTimer--;
		if (swapTimer == 60 || swapTimer == 40 || swapTimer == 20) {
			countdown(server, swapTimer / 20);
		}
		if (swapTimer <= 0) {
			swapTimer = SWAP_TICKS;
			swapPlayers(server, players);
		}
	}

	private static void countdown(MinecraftServer server, int n) {
		CommandSourceStack src = server.createCommandSourceStack().withSuppressedOutput();
		var cmds = server.getCommands();
		cmds.performPrefixedCommand(src, "title @a times 0 25 5");
		cmds.performPrefixedCommand(src, "title @a title {text:\"" + n + "\",color:\"gold\",bold:true}");
		cmds.performPrefixedCommand(src, "execute as @a at @s run playsound minecraft:block.note_block.pling master @s ~ ~ ~ 1 1");
	}

	private static void swapPlayers(MinecraftServer server, List<ServerPlayer> players) {
		int n = players.size();
		if (n < 2) return;

		ServerLevel[] levels = new ServerLevel[n];
		double[] xs = new double[n], ys = new double[n], zs = new double[n];
		for (int i = 0; i < n; i++) {
			ServerPlayer p = players.get(i);
			levels[i] = (ServerLevel) p.level();
			xs[i] = p.getX();
			ys[i] = p.getY();
			zs[i] = p.getZ();
		}
		// كل لاعب ياخذ مكان اللي بعده (مع 2 لاعبين = تبديل مباشر)
		for (int i = 0; i < n; i++) {
			int j = (i + 1) % n;
			ServerPlayer p = players.get(i);
			p.teleportTo(levels[j], xs[j], ys[j], zs[j], Set.<Relative>of(), p.getYRot(), p.getXRot(), false);
		}

		CommandSourceStack src = server.createCommandSourceStack().withSuppressedOutput();
		server.getCommands().performPrefixedCommand(src, "title @a times 0 30 10");
		server.getCommands().performPrefixedCommand(src, "title @a title {text:\"SWAP!\",color:\"red\",bold:true}");
		server.getCommands().performPrefixedCommand(src, "execute as @a at @s run playsound minecraft:entity.enderman.teleport master @s ~ ~ ~ 1 1");
	}

	// ---------------- اللكي بلوك ----------------

	private static void giveLuckyBlock(ServerPlayer p) {
		ItemStack stack = new ItemStack(Items.YELLOW_GLAZED_TERRACOTTA);
		stack.set(DataComponents.ITEM_NAME,
				Component.literal("Lucky Block").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		if (!p.getInventory().add(stack)) {
			p.drop(stack, false);
		}
	}

	private interface Effect {
		void apply(ServerLevel level, Vec3 at, ServerPlayer player);
	}

	private static final List<Effect> EFFECTS = new ArrayList<>();
	private static List<Item> dropPool;

	static {
		// أغراض عشوائية
		EFFECTS.add((l, at, p) -> { for (int i = 0; i < 3; i++) dropRandomItem(l, at); });
		EFFECTS.add((l, at, p) -> { for (int i = 0; i < 6; i++) dropRandomItem(l, at); });
		// ألماس
		EFFECTS.add((l, at, p) -> drop(l, at, new ItemStack(Items.DIAMOND, 5 + RNG.nextInt(8))));
		// نذرايت
		EFFECTS.add((l, at, p) -> {
			drop(l, at, new ItemStack(Items.NETHERITE_HELMET));
			drop(l, at, new ItemStack(Items.NETHERITE_CHESTPLATE));
			drop(l, at, new ItemStack(Items.NETHERITE_LEGGINGS));
			drop(l, at, new ItemStack(Items.NETHERITE_BOOTS));
		});
		// تفاحة ذهبية سحرية
		EFFECTS.add((l, at, p) -> drop(l, at, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE)));
		// أكل
		EFFECTS.add((l, at, p) -> drop(l, at, new ItemStack(Items.COOKED_BEEF, 16)));
		// مخلوقات
		EFFECTS.add((l, at, p) -> { for (int i = 0; i < 3; i++) run(l, at, "summon minecraft:creeper"); });
		EFFECTS.add((l, at, p) -> { for (int i = 0; i < 4; i++) run(l, at, "summon minecraft:zombie"); });
		EFFECTS.add((l, at, p) -> { for (int i = 0; i < 3; i++) run(l, at, "summon minecraft:skeleton"); });
		EFFECTS.add((l, at, p) -> run(l, at, "summon minecraft:iron_golem"));
		EFFECTS.add((l, at, p) -> run(l, at, "summon minecraft:wither_skeleton"));
		EFFECTS.add((l, at, p) -> run(l, at, "summon minecraft:witch"));
		EFFECTS.add((l, at, p) -> { for (int i = 0; i < 6; i++) run(l, at, "summon minecraft:cow"); });
		EFFECTS.add((l, at, p) -> { for (int i = 0; i < 5; i++) run(l, at, "summon minecraft:chicken"); });
		// تفجير
		EFFECTS.add((l, at, p) -> { for (int i = 0; i < 3; i++) run(l, at, "summon minecraft:tnt ~ ~1 ~ {fuse:40s}"); });
		EFFECTS.add((l, at, p) -> run(l, at, "summon minecraft:lightning_bolt"));
		// تأثيرات على اللاعب
		EFFECTS.add((l, at, p) -> effect(l, at, p, "speed 30 2"));
		EFFECTS.add((l, at, p) -> effect(l, at, p, "jump_boost 30 4"));
		EFFECTS.add((l, at, p) -> effect(l, at, p, "levitation 5 1"));
		EFFECTS.add((l, at, p) -> effect(l, at, p, "regeneration 15 2"));
		EFFECTS.add((l, at, p) -> effect(l, at, p, "blindness 8 1"));
		// مقالب
		EFFECTS.add((l, at, p) -> run(l, at, "setblock ~ ~6 ~ minecraft:anvil"));
		EFFECTS.add((l, at, p) -> run(l, at, "setblock ~ ~ ~ minecraft:lava"));
		EFFECTS.add((l, at, p) -> run(l, at, "fill ~-1 ~ ~-1 ~1 ~2 ~1 minecraft:cobweb replace minecraft:air"));
		EFFECTS.add((l, at, p) -> run(l, at, "summon minecraft:experience_orb ~ ~ ~ {Value:200s}"));
		// خيبة
		EFFECTS.add((l, at, p) -> drop(l, at, new ItemStack(Items.DIRT, 1)));
	}

	private static void openLucky(ServerLevel level, BlockPos pos, ServerPlayer player) {
		Vec3 at = Vec3.atCenterOf(pos);
		EFFECTS.get(RNG.nextInt(EFFECTS.size())).apply(level, at, player);
	}

	// ---------------- أدوات مساعدة ----------------

	private static void run(ServerLevel level, Vec3 at, String cmd) {
		MinecraftServer server = level.getServer();
		CommandSourceStack src = server.createCommandSourceStack()
				.withLevel(level).withPosition(at).withSuppressedOutput();
		server.getCommands().performPrefixedCommand(src, cmd);
	}

	private static void effect(ServerLevel level, Vec3 at, ServerPlayer p, String spec) {
		run(level, at, "effect give " + p.getUUID() + " minecraft:" + spec);
	}

	private static void drop(ServerLevel level, Vec3 at, ItemStack stack) {
		level.addFreshEntity(new ItemEntity(level, at.x, at.y + 0.3, at.z, stack));
	}

	private static void dropRandomItem(ServerLevel level, Vec3 at) {
		if (dropPool == null) {
			dropPool = new ArrayList<>();
			BuiltInRegistries.ITEM.stream().forEach(item -> {
				String id = BuiltInRegistries.ITEM.getKey(item).toString();
				if (id.equals("minecraft:air") || id.equals("minecraft:light")
						|| id.contains("command_block") || id.contains("barrier")
						|| id.contains("structure_") || id.contains("jigsaw")
						|| id.contains("debug_stick") || id.contains("knowledge_book")) return;
				dropPool.add(item);
			});
		}
		Item item = dropPool.get(RNG.nextInt(dropPool.size()));
		int count = 1 + RNG.nextInt(Math.min(8, item.getDefaultMaxStackSize()));
		drop(level, at, new ItemStack(item, count));
	}
}

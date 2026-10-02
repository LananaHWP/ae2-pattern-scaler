package dev.local.ae2patternscaler.client;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.me.items.PatternEncodingTermScreen;
import appeng.core.network.serverbound.InventoryActionPacket;
import appeng.helpers.InventoryAction;
import appeng.parts.encoding.EncodingMode;

import dev.local.ae2patternscaler.AmountScaler;

public final class PatternTerminalControls {
    private static final long MAX_DISPLAY_UNITS = 999_999L;
    private static final int BUTTON_WIDTH = 28;
    private static final int BUTTON_HEIGHT = 18;
    private static final int GAP = 2;
    private static final int COLUMNS = 2;
    private static final int PATTERN_SLOTS_BOTTOM_OFFSET = 158;

    private static final List<Operation> OPERATIONS = List.of(
            new Operation(AmountScaler.Direction.MULTIPLY, 2),
            new Operation(AmountScaler.Direction.DIVIDE, 2),
            new Operation(AmountScaler.Direction.MULTIPLY, 4),
            new Operation(AmountScaler.Direction.DIVIDE, 4),
            new Operation(AmountScaler.Direction.MULTIPLY, 8),
            new Operation(AmountScaler.Direction.DIVIDE, 8),
            new Operation(AmountScaler.Direction.MULTIPLY, 16),
            new Operation(AmountScaler.Direction.DIVIDE, 16),
            new Operation(AmountScaler.Direction.MULTIPLY, 32),
            new Operation(AmountScaler.Direction.DIVIDE, 32),
            new Operation(AmountScaler.Direction.MULTIPLY, 64),
            new Operation(AmountScaler.Direction.DIVIDE, 64));

    private static final Map<PatternEncodingTermScreen<?>, List<Button>> CONTROLS = new IdentityHashMap<>();

    private PatternTerminalControls() {
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PatternEncodingTermScreen<?> screen)) {
            return;
        }

        int panelWidth = COLUMNS * BUTTON_WIDTH + (COLUMNS - 1) * GAP;
        int rows = (OPERATIONS.size() + COLUMNS - 1) / COLUMNS;
        int panelHeight = rows * BUTTON_HEIGHT + (rows - 1) * GAP;
        int leftX = screen.getGuiLeft() - panelWidth - 4;
        int rightX = screen.getGuiLeft() + screen.getXSize() + 4;

        final int startX;
        if (leftX >= 0) {
            startX = leftX;
        } else if (rightX + panelWidth <= screen.width) {
            startX = rightX;
        } else {
            // Hide instead of covering the terminal.
            CONTROLS.remove(screen);
            return;
        }

        int preferredY = screen.getGuiTop() + screen.getYSize() - PATTERN_SLOTS_BOTTOM_OFFSET;
        int startY = Math.max(4, Math.min(preferredY, screen.height - panelHeight - 4));
        boolean processing = screen.getMenu().getMode() == EncodingMode.PROCESSING;
        List<Button> buttons = new ArrayList<>(OPERATIONS.size());

        for (int i = 0; i < OPERATIONS.size(); i++) {
            Operation operation = OPERATIONS.get(i);
            int x = startX + (i % COLUMNS) * (BUTTON_WIDTH + GAP);
            int y = startY + (i / COLUMNS) * (BUTTON_HEIGHT + GAP);

            Button button = Button.builder(
                    Component.literal(operation.label()),
                    ignored -> apply(screen, operation))
                    .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .tooltip(Tooltip.create(Component.translatable(operation.tooltipKey(), operation.factor())))
                    .build();
            button.visible = processing;
            button.active = processing;
            buttons.add(button);
            event.addListener(button);
        }

        CONTROLS.put(screen, buttons);
    }

    public static void onScreenRender(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof PatternEncodingTermScreen<?> screen)) {
            return;
        }

        List<Button> buttons = CONTROLS.get(screen);
        if (buttons == null) {
            return;
        }

        boolean processing = screen.getMenu().getMode() == EncodingMode.PROCESSING;
        for (Button button : buttons) {
            button.visible = processing;
            button.active = processing;
        }
    }

    public static void onScreenClosing(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof PatternEncodingTermScreen<?> screen) {
            CONTROLS.remove(screen);
        }
    }

    private static void apply(PatternEncodingTermScreen<?> screen, Operation operation) {
        if (screen.getMenu().getMode() != EncodingMode.PROCESSING) {
            return;
        }

        List<SlotUpdate> updates = new ArrayList<>();
        if (!collectUpdates(operation, Arrays.asList(screen.getMenu().getProcessingInputSlots()), updates)) {
            return;
        }
        if (!collectUpdates(operation, Arrays.asList(screen.getMenu().getProcessingOutputSlots()), updates)) {
            return;
        }

        if (updates.isEmpty()) {
            showMessage(Component.translatable("ae2_pattern_scaler.message.empty"));
            return;
        }

        // Use AE2's normal slot update.
        for (SlotUpdate update : updates) {
            var packet = new InventoryActionPacket(
                    InventoryAction.SET_FILTER,
                    update.slot().index,
                    GenericStack.wrapInItemStack(update.stack()));
            PacketDistributor.sendToServer(packet);
        }

        showMessage(Component.translatable(
                "ae2_pattern_scaler.message.success",
                operation.label(),
                updates.size()));
    }

    private static boolean collectUpdates(
            Operation operation,
            List<? extends Slot> slots,
            List<SlotUpdate> updates) {
        for (Slot slot : slots) {
            GenericStack current = GenericStack.fromItemStack(slot.getItem());
            if (current == null) {
                continue;
            }

            long maxAmount = normalMaximum(current);
            AmountScaler.Result result = AmountScaler.scale(
                    current.amount(),
                    maxAmount,
                    operation.direction(),
                    operation.factor());

            if (!result.successful()) {
                reportFailure(current, maxAmount, operation, result.failure());
                return false;
            }

            updates.add(new SlotUpdate(slot, new GenericStack(current.what(), result.amount())));
        }
        return true;
    }

    private static long normalMaximum(GenericStack stack) {
        return Math.multiplyExact(MAX_DISPLAY_UNITS, stack.what().getAmountPerUnit());
    }

    private static void reportFailure(
            GenericStack stack,
            long maxAmount,
            Operation operation,
            AmountScaler.Failure failure) {
        Component resource = stack.what().getDisplayName();
        Component message = switch (failure) {
            case LIMIT_EXCEEDED -> Component.translatable(
                    "ae2_pattern_scaler.message.limit",
                    operation.label(),
                    resource,
                    formatMaximum(stack, maxAmount));
            case NOT_EXACT -> Component.translatable(
                    "ae2_pattern_scaler.message.inexact",
                    operation.label(),
                    resource,
                    formatStoredAmount(stack),
                    storedPrecision(stack));
            case WOULD_BECOME_ZERO -> Component.translatable(
                    "ae2_pattern_scaler.message.zero",
                    operation.label(),
                    resource);
            case NONE -> throw new IllegalArgumentException("Cannot report a successful result as a failure");
        };
        showMessage(message);
    }

    private static String formatMaximum(GenericStack stack, long maxAmount) {
        NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.getDefault());
        if (stack.what() instanceof AEFluidKey) {
            return numbers.format(maxAmount / stack.what().getAmountPerUnit()) + " B";
        }
        return numbers.format(maxAmount) + " items";
    }

    private static String formatStoredAmount(GenericStack stack) {
        NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.getDefault());
        return numbers.format(stack.amount()) + (stack.what() instanceof AEFluidKey ? " mB" : " items");
    }

    private static String storedPrecision(GenericStack stack) {
        return stack.what() instanceof AEFluidKey ? "1 mB" : "whole items";
    }

    private static void showMessage(Component message) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(message, true);
        }
    }

    private record SlotUpdate(Slot slot, GenericStack stack) {
    }

    private record Operation(AmountScaler.Direction direction, int factor) {
        String label() {
            return (direction == AmountScaler.Direction.MULTIPLY ? "\u00d7" : "\u00f7") + factor;
        }

        String tooltipKey() {
            return direction == AmountScaler.Direction.MULTIPLY
                    ? "ae2_pattern_scaler.tooltip.multiply"
                    : "ae2_pattern_scaler.tooltip.divide";
        }
    }
}

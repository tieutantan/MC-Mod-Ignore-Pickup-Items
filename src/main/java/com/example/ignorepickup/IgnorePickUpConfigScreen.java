package com.example.ignorepickup;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class IgnorePickUpConfigScreen extends Screen {
    private EditBox searchBox;
    private java.util.List<String> allItems;        // full ids (e.g. "minecraft:stone")
    private java.util.List<String> allItemsLower;   // cached lowercased ids
    private java.util.List<String> viewItems;       // current view
    private final java.util.List<Button> itemButtons = new java.util.ArrayList<>();
    private int page = 0;
    private int pageSize = 30; // 3 columns x 10 rows
    private Button prevBtn;
    private Button nextBtn;

    public IgnorePickUpConfigScreen() {
        super(Component.literal("Ignore Pickup Config"));
    }

    @Override
    protected void init() {
    // Batch config IO while interacting with UI
    Config.startBatch();

    // Build the complete list of item ids
        allItems = new java.util.ArrayList<>();
    allItemsLower = new java.util.ArrayList<>();
        for (var item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null) {
                String s = id.toString();
                allItems.add(s);
                // Track in known list for persistence/commands
        Config.addKnownEphemeral(s);
            }
        }
    allItems.sort(String::compareTo);
    for (String s : allItems) allItemsLower.add(s.toLowerCase());

    updateView();

    int margin = 12;
        int centerX = this.width / 2;

        searchBox = new EditBox(this.font, margin, 20, this.width - margin * 2 - 180, 20, Component.literal("Search"));
        searchBox.setMaxLength(256);
    searchBox.setResponder(this::applyFilter);
        addRenderableWidget(searchBox);

        prevBtn = addRenderableWidget(Button.builder(Component.literal("< Prev"), b -> {
            if (page > 0) { page--; rebuildButtons(); }
        }).pos(this.width - 170, 20).size(70, 20).build());

        nextBtn = addRenderableWidget(Button.builder(Component.literal("Next >"), b -> {
            int size = viewItems == null ? 0 : viewItems.size();
            int maxPage = Math.max(0, (size - 1) / pageSize);
            if (page < maxPage) { page++; rebuildButtons(); }
        }).pos(this.width - 95, 20).size(70, 20).build());

        // Done button
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .pos(centerX - 40, this.height - 28).size(80, 20).build());

        rebuildButtons();
    }

    private void applyFilter(String query) {
        updateView();
        page = 0;
        rebuildButtons();
    }

    private void updateView() {
        String q = searchBox == null ? "" : (searchBox.getValue() == null ? "" : searchBox.getValue().toLowerCase());
        java.util.Set<String> active = Config.getIgnoredActive();
        java.util.ArrayList<String> list = new java.util.ArrayList<>();
        if (q.isEmpty()) {
            // Default: show only blocked items
            list.addAll(active);
        } else {
            // Search mode: show all items matching query
            int n = allItems.size();
            for (int i = 0; i < n; i++) {
                if (allItemsLower.get(i).contains(q)) list.add(allItems.get(i));
            }
        }
        list.sort(String::compareTo);
        viewItems = list;
    }

    private void rebuildButtons() {
        // Remove old item buttons
        for (Button btn : itemButtons) {
            this.removeWidget(btn);
        }
        itemButtons.clear();

        // Layout grid
        int columns = 3;
        int rows = 10;
        pageSize = columns * rows;
        int gridLeft = 16;
        int gridRight = this.width - 16;
    int gridTop = 71; // shifted down to sit ~20px below the info line at y=51
        int cellW = (gridRight - gridLeft - (columns - 1) * 6) / columns;
        int cellH = 18;

        java.util.List<String> list = viewItems == null ? java.util.List.of() : viewItems;
        int start = page * pageSize;
        int end = Math.min(list.size(), start + pageSize);

        java.util.Set<String> active = Config.getIgnoredActive();

        for (int idx = start; idx < end; idx++) {
            String id = list.get(idx);
            int rel = idx - start;
            int row = rel / columns;
            int col = rel % columns;
            int x = gridLeft + col * (cellW + 6);
            int y = gridTop + row * (cellH + 4);

            boolean isBlocked = active.contains(id);
            String display = id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
            Component label = Component.literal((isBlocked ? "[X] " : "[ ] ") + display);
            Button btn = Button.builder(label, b -> {
                Config.toggleIgnored(id);
                // After toggle, refresh the view:
                updateView();
                rebuildButtons();
            }).pos(x, y).size(cellW, cellH).build();
            itemButtons.add(addRenderableWidget(btn));
        }

        // Update prev/next enabled states
        int maxPage = Math.max(0, ((viewItems == null ? 0 : viewItems.size()) - 1) / pageSize);
        prevBtn.active = page > 0;
        nextBtn.active = page < maxPage;
    }

    @Override
    public void onClose() {
        Config.save();
    Config.endBatch();
        super.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 6, 0xFFFFFF);
        // Page info
    int total = viewItems == null ? 0 : viewItems.size();
        int maxPage = total == 0 ? 0 : (total - 1) / pageSize;
    String info = "Items: " + total + "  Page: " + (Math.min(page, maxPage) + 1) + "/" + (maxPage + 1);
    guiGraphics.drawString(this.font, info, 16, 51, 0xAAAAAA);
    }
}

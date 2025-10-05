package com.candyrealms.candycore.modules.staff;

import com.candyrealms.candycore.CandyCore;
import com.candyrealms.candycore.configuration.ConfigManager;
import com.candyrealms.candycore.modules.staff.enums.ChatType;
import com.candyrealms.candycore.modules.staff.modules.FreezeModule;
import com.candyrealms.candycore.modules.staff.modules.RandomTPModule;
import com.candyrealms.candycore.modules.staff.modules.StaffModeModule;
import com.candyrealms.candycore.utils.ColorUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StaffModule {

    private final ConfigManager config;

    @Getter
    private final FreezeModule freezeModule;

    @Getter
    private final RandomTPModule randomTPModule;

    @Getter
    private final StaffModeModule staffModeModule;

    @Getter
    @Setter
    private boolean muted;

    private final Map<UUID, ChatType> chatTypeMap = new HashMap<>();

    public StaffModule(CandyCore plugin) {
        config = plugin.getConfigManager();

        staffModeModule = new StaffModeModule(plugin);
        freezeModule = new FreezeModule();
        randomTPModule = new RandomTPModule();
    }

    public boolean inSpecialChat(Player player) {
        return chatTypeMap.containsKey(player.getUniqueId());
    }

    public void sendSpecialMsg(Player player, String string) {
        ChatType chatType = chatTypeMap.get(player.getUniqueId());

        List<Player> playerList = Bukkit.getOnlinePlayers().stream()
                .filter(p -> (chatType == ChatType.STAFF) ? p.hasPermission("candycore.staffchat") : p.hasPermission("candycore.adminchat"))
                .collect(Collectors.toList());

        String prefix = (chatType == ChatType.STAFF) ? "&b&lSC &7&o(" + player.getName() + ")" + " &3:&f"
                : "&c&lAC &7&o(" + player.getName() + ")" + " &4:&f";

        playerList.forEach(p -> p.sendMessage(ColorUtil.color(prefix + " " + string)));
    }

    public void toggleStaffChat(Player player) {
        if(!chatTypeMap.containsKey(player.getUniqueId()) || chatTypeMap.get(player.getUniqueId()) != ChatType.STAFF) {
            chatTypeMap.put(player.getUniqueId(), ChatType.STAFF);
            player.sendMessage(ColorUtil.color("&e&lSTAFF &fYou have &a&nenabled&f staffchat!"));
            player.playSound(player.getLocation(), Sound.ORB_PICKUP, 7, 6);
            return;
        }

        chatTypeMap.remove(player.getUniqueId());
        player.sendMessage(ColorUtil.color("&e&lSTAFF &fYou have &c&ndisabled&f staffchat!"));
        player.playSound(player.getLocation(), Sound.ANVIL_LAND, 7, 6);
    }

    public void toggleAdminChat(Player player) {
        if(!chatTypeMap.containsKey(player.getUniqueId()) || chatTypeMap.get(player.getUniqueId()) != ChatType.ADMIN) {
            chatTypeMap.put(player.getUniqueId(), ChatType.ADMIN);
            player.sendMessage(ColorUtil.color("&e&lSTAFF &fYou have &a&nenabled&f adminchat!"));
            player.playSound(player.getLocation(), Sound.ORB_PICKUP, 7, 6);
            return;
        }

        chatTypeMap.remove(player.getUniqueId());
        player.sendMessage(ColorUtil.color("&e&lSTAFF &fYou have &c&ndisabled&f adminchat!"));
        player.playSound(player.getLocation(), Sound.ANVIL_LAND, 7, 6);
    }

    public void clearChat() {
        IntStream.range(0, 120).forEach(i -> Bukkit.broadcastMessage(" "));

        config.announceChatCleared();
    }

    public void toggleMute() {
        if(isMuted()) {
            setMuted(false);
            config.announceChatUnmute();
            return;
        }

        setMuted(true);
        config.announceChatMute();
    }
}

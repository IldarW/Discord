package net.Ildar.wurm;

import com.wurmonline.client.renderer.gui.*;
import com.wurmonline.shared.util.MulticolorLineSegment;
import javassist.ClassPool;
import javassist.CtClass;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmClientMod;

import javax.security.auth.login.LoginException;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Discord extends ListenerAdapter implements WurmClientMod, Initable, Configurable {
    private static final String ON_MESSAGE = ":ok_hand: Bot is on :ok_hand:";
    private static final String OFF_MESSAGE = ":zzz: Bot is off :zzz:";

    private final String TOLD_IN_DISCORD = "told in discord";
    private final String CONVEYED_FROM_DISCORD = "conveyed from";
    private final String CONVEYED_TO_DISCORD = "conveyed to";
    private final String PLAYERS_LIST = "the list of online players";
    private final String TOO_QUICK_WHO_MESSAGE = "Too quick. Try to wait a little.";
    private final String INTHEGAME_MESSAGE = ":video_game::video_game::video_game:%nOnline players - %s%n:video_game::video_game::video_game:";

    private static Logger logger = Logger.getLogger(Discord.class.getSimpleName());
    private static HeadsUpDisplay hud;
    private static JDA jda;
    private static String botToken;
    private static String serverName;
    private static String villageChannel;
    private static Discord instance;
    private long lastTimeWhoIsOnlineChecked = 0;
    private static List<Function<String, Boolean>> filters = new ArrayList<>();

    //handle ingame chat message
    public static void  onGameMessage(String context, Object input, boolean silent) {
        String message;
        if (!"Village".equals(context)) 
            return;
        if (input instanceof List) {
            StringBuilder sb = new StringBuilder();
            for (Iterator<MulticolorLineSegment> iter = ((List)input).iterator(); iter.hasNext(); ) {
                MulticolorLineSegment segment = iter.next();
                sb.append(segment.getText());
            }
            message = sb.toString();
        } else
            message = (String)input;
        message = message.substring(11).trim();
        if (message.isEmpty()) return;
        for (Function filter : filters)
            try {
                if ((boolean) filter.apply( message))
                    return;
            } catch (PatternSyntaxException e) {
                logger.severe(e.getMessage());
                return;
            }
        sendDiscordMessage(message);
    }

    public static void disconnect() {
        if (jda == null) {
            hud.consoleOutput("Discord is not on.");
            return;
        }
        sendDiscordMessage(OFF_MESSAGE);
        jda.shutdown();
        jda = null;
        hud.consoleOutput("Discord is off");
    }

    //...
    private static void sendDiscordMessage(String message) {
        if (jda == null)
            return;
        MessageBuilder builder = new MessageBuilder();
        builder.append(message);
        try {
            jda.getGuildsByName(serverName, true)
                    .get(0)
                    .getTextChannelsByName(villageChannel, true)
                    .get(0)
                    .sendMessage(builder.build()).queue();
        } catch(Exception e) {
            hud.consoleOutput("Exception in sendDiscordMessage() - " + e.getMessage());
            e.printStackTrace();
        }
    }

    //get comma separated list of players in Village tab
    public static String getOnlinePlayers(){
        List<String> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        try {
            ChatManagerManager cmm = ReflectionUtil.getPrivateField(Discord.hud,
                    ReflectionUtil.getField(Discord.hud.getClass(), "chatManager"));
            Object ccp = ReflectionUtil.getPrivateField(cmm,
                    ReflectionUtil.getField(cmm.getClass(), "chatChatPanel"));
            Map<String, AbstractTab> tabs = ReflectionUtil.getPrivateField(ccp,
                    ReflectionUtil.getField(ccp.getClass(), "tabs"));
            AbstractTab tab = tabs.get("Village");
            Object memberList = ReflectionUtil.getPrivateField(tab,
                    ReflectionUtil.getField(tab.getClass(), "memberList"));
            List lines = ReflectionUtil.getPrivateField(memberList,
                    ReflectionUtil.getField(memberList.getClass(), "lines"));
            for(Iterator iter = lines.iterator(); iter.hasNext();) {
                Object iterobj = iter.next();
                String member = ReflectionUtil.getPrivateField(iterobj,
                        ReflectionUtil.getField(iterobj.getClass(), "text"));
                if(!list.contains(member))
                    list.add(member);
            }
            list.add(playerName());
        } catch (Exception e) {
            hud.consoleOutput("Unexpected error while counting players");
            hud.consoleOutput(e.toString());
        }
        list.sort(Comparator.naturalOrder());
        for(Iterator iter = list.iterator(); iter.hasNext();) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(iter.next());
        }
        return sb.toString();
    }

    //handle console command
    public static boolean handleInput(final String cmd, final String[] data) {
        switch (cmd) {
            case "discord":
                String usage = "Usage: discord {on|off}";
                if (data.length == 2) {
                    switch (data[1]){
                        case "on":
                            if (jda != null) {
                                hud.consoleOutput("Discord is already on.");
                                return true;
                            } else {
                                new Thread(() ->{
                                    try {
                                        jda = new JDABuilder(AccountType.BOT)
                                                .setToken(botToken)
                                                .addEventListener(instance)
                                                .buildBlocking();
                                        sendDiscordMessage(ON_MESSAGE);
                                        Discord.hud.consoleOutput("Discord is on.");
                                    } catch (LoginException | InterruptedException | RateLimitedException e) {
                                        hud.consoleOutput("Error - " + e.getMessage());
                                        e.printStackTrace();
                                    }
                                }).start();
                            }
                            break;
                        case "off":
                            disconnect();
                            break;
                    }
                }
                return true;
            default:
                return false;
        }
    }


    //handle discord message
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        super.onMessageReceived(event);
        if(jda == null) return;
        if (event.isFromType(ChannelType.TEXT) && !event.getAuthor().isBot()) {
            if (event.getChannel().getName().equals(villageChannel)) {
                String message;
                String wurmChannel = "Village";
                if (event.getMessage().getContentDisplay().equals("!who")) {
                    if (Math.abs(lastTimeWhoIsOnlineChecked - System.currentTimeMillis()) < 10000) {
                        hud.consoleOutput(TOO_QUICK_WHO_MESSAGE + " Time - " + lastTimeWhoIsOnlineChecked);
                        sendDiscordMessage(TOO_QUICK_WHO_MESSAGE);
                        return;
                    }
                    lastTimeWhoIsOnlineChecked = System.currentTimeMillis();
                    hud.consoleOutput("!who executed. Time - " + lastTimeWhoIsOnlineChecked);
                    String onlinemembers = getOnlinePlayers();
                    if (onlinemembers != null && !onlinemembers.isEmpty()) {
                        if (!playerName().equals(event.getAuthor().getName()))
                            hud.getWorld().getServerConnection().sendmessage5("Village", "/me " + CONVEYED_TO_DISCORD +" [" + event.getAuthor().getName() + "] " + PLAYERS_LIST);
                        message = String.format(INTHEGAME_MESSAGE, onlinemembers);
                        sendDiscordMessage(message);
                    }
                } else {
                    if (event.getAuthor().getName().equals(playerName()))
                        message = String.format("/me " + TOLD_IN_DISCORD + ": %s\n", event.getMessage().getContentDisplay());
                    else
                        message = String.format("/me " + CONVEYED_FROM_DISCORD + " [%s]: %s\n", event.getAuthor().getName(), event.getMessage().getContentDisplay());
                    hud.getWorld().getServerConnection().sendmessage5(wurmChannel, message);
                }
            }
        }
    }
    
    private static String playerName() {
        return hud.getWorld().getPlayer().getPlayerName();
    }


    @Override
    public void configure(Properties properties) {
        botToken = properties.getProperty("discordBotToken");
        serverName = properties.getProperty("discordServer");
        villageChannel = properties.getProperty("discordVillageChannel");
        logger.info("Config loaded");
        instance = this;
    }
    @Override
    public void init() {
        try {
            final ClassPool classPool = HookManager.getInstance().getClassPool();
            final CtClass ctWurmConsole = classPool.getCtClass("com.wurmonline.client.console.WurmConsole");
            ctWurmConsole.getMethod("handleDevInput", "(Ljava/lang/String;[Ljava/lang/String;)Z").insertBefore("if (net.Ildar.wurm.Discord.handleInput($1,$2)) return true;");
            HookManager.getInstance().registerHook("com.wurmonline.client.renderer.gui.HeadsUpDisplay", "init", "(II)V", () -> (proxy, method, args) -> {
                method.invoke(proxy, args);
                hud = (HeadsUpDisplay)proxy;
                return null;
            });
            filters.add(message -> Pattern.matches(playerName() + " " + CONVEYED_FROM_DISCORD + " \\[.+\\]:.*", message));
            filters.add(message -> Pattern.matches(playerName() + " " + TOLD_IN_DISCORD + ":.*", message));
            filters.add(message -> Pattern.matches(playerName() + " " + CONVEYED_TO_DISCORD + " \\[.+\\] " + PLAYERS_LIST + ".*", message));

            final CtClass ctWurmClientBase = classPool.getCtClass("com.wurmonline.client.WurmClientBase");
            ctWurmClientBase.getMethod("performShutdown", "()V").insertBefore("net.Ildar.wurm.Discord.disconnect();");
            final CtClass errorPanel = classPool.getCtClass("com.wurmonline.client.ErrorReporterPanel");
            errorPanel.getMethod("run", "()V").insertBefore("net.Ildar.wurm.Discord.disconnect();");

            final CtClass ctWurmChat = classPool.getCtClass("com.wurmonline.client.renderer.gui.ChatPanelComponent");
            ctWurmChat.getMethod("addText", "(Ljava/lang/String;Ljava/util/List;Z)V").insertBefore("net.Ildar.wurm.Discord.onGameMessage($1,$2,$3);");
            ctWurmChat.getMethod("addText", "(Ljava/lang/String;Ljava/lang/String;FFFZ)V").insertBefore("net.Ildar.wurm.Discord.onGameMessage($1,$2,$6);");
            logger.info("initiated");
        } catch (Exception e) {
            if (Discord.logger != null) {
                Discord.logger.log(Level.SEVERE, "Error loading mod", e);
                Discord.logger.log(Level.SEVERE, e.getMessage());
            }
        }
    }
}

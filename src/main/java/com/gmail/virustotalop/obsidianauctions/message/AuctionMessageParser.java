package com.gmail.virustotalop.obsidianauctions.message;

import com.gmail.virustotalop.obsidianauctions.AuctionConfig;
import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.auction.Auction;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionBid;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionScope;
import com.gmail.virustotalop.obsidianauctions.language.TranslationFactory;
import com.gmail.virustotalop.obsidianauctions.util.Functions;
import com.gmail.virustotalop.obsidianauctions.util.Items;
import com.gmail.virustotalop.obsidianauctions.util.PlaceholderAPIUtil;
import com.google.inject.Inject;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AuctionMessageParser {

    private final TranslationFactory translation;

    @Inject
    private AuctionMessageParser(TranslationFactory translation) {
        this.translation = translation;
    }

    /**
     * Gets the messages from the language.yml file based on the keys passed in.
     *
     * @param messageKeys  Keys specified in the language.yml file
     * @param auctionScope A scope to check for local messages
     * @return List of actual messages to send
     */
    public List<String> parseMessages(List<String> messageKeys, AuctionScope auctionScope, Auction auction, Player player, boolean isBroadcast) {
        List<String> messageList = new ArrayList<>();

        for(int l = 0; l < messageKeys.size(); l++) {
            String messageKey = messageKeys.get(l);
            if(messageKey == null) {
                continue;
            }

            List<String> partialMessageList = AuctionConfig.getLanguageStringList(messageKey, auctionScope);

            if(partialMessageList == null || partialMessageList.size() == 0) {
                String originalMessage = null;
                originalMessage = AuctionConfig.getLanguageString(messageKey, auctionScope);

                if(originalMessage == null || originalMessage.length() == 0) {
                    continue;
                } else {
                    partialMessageList = Arrays.asList(originalMessage.split("(\r?\n|\r)"));
                }
            }
            messageList.addAll(partialMessageList);
        }
        return parseMessageTokens(messageList, auctionScope, auction, player, isBroadcast);
    }

    private List<String> parseMessageTokens(List<String> messageList, AuctionScope auctionScope, Auction auction, Player player, boolean isBroadcast) {
        List<String> newMessageList = new ArrayList<>();
        Map<String, String> replacements = new HashMap<>();
        ItemStack lot = null;

        if(auction == null && auctionScope != null) {
            auction = auctionScope.getActiveAuction();
        }

        // Search to see if auction info is required:
        for(int l = 0; l < messageList.size(); l++) {
            String message = messageList.get(l);
            if(message.length() > 0 && message.contains("%auction-")) {
                if(auction != null) {
                    replacements.put("%auction-owner-name%", auction.getOwnerName()); //%A1
                    replacements.put("%auction-owner-display-name%", auction.getOwnerDisplayName()); //%A2
                    replacements.put("%auction-quantity%", Integer.toString(auction.getLotQuantity()));
                    if(auction.getStartingBid() == 0) {
                        replacements.put("%auction-bid-starting%", Functions.formatAmount(auction.getMinBidIncrement())); //%A4
                    } else {
                        replacements.put("%auction-bid-starting%", Functions.formatAmount(auction.getStartingBid())); //%A4
                    }
                    replacements.put("%auction-bid-increment%", Functions.formatAmount(auction.getMinBidIncrement())); //%A5
                    replacements.put("%auction-buy-now%", Functions.formatAmount(auction.getBuyNow())); //%A6
                    replacements.put("%auction-remaining-time%", Functions.formatTime(auction.getRemainingTime(), auctionScope)); //%A7
                    replacements.put("%auction-pre-tax%", Functions.formatAmount(auction.extractedPreTax)); //%A8
                    replacements.put("%auction-post-tax%", Functions.formatAmount(auction.extractedPostTax)); //%A9
                }
                break;
            }
        }

        // Search to see if auction bid info is required:
        for(int l = 0; l < messageList.size(); l++) {
            String message = messageList.get(l);
            if(message.length() > 0 && (message.contains("%current-") || message.contains("%auction-bid"))) {
                if(auction != null) {
                    AuctionBid currentBid = auction.getCurrentBid();
                    if(currentBid != null) {
                        replacements.put("%current-bid-name%", currentBid.getBidderName()); //%B1
                        replacements.put("%current-bid-display-name%", currentBid.getBidderDisplayName()); //%B2
                        replacements.put("%current-bid-amount%", Functions.formatAmount(currentBid.getBidAmount())); //%B3
                        replacements.put("%auction-bid-starting%", Functions.formatAmount(auction.getStartingBid())); //%B4
                    } else {
                        String bidderName = ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("auction-info-bidder-noone", auctionScope));
                        String startingBid = Functions.formatAmount(auction.getStartingBid());
                        replacements.put("%current-bid-name%", bidderName); //%B1
                        replacements.put("%current-bid-display-name%", bidderName); //%B2
                        replacements.put("%current-bid-amount%", startingBid); //%B3
                        replacements.put("%auction-bid-starting%", startingBid); //%B4
                    }
                }
                break;
            }
        }

        // Search to see if auction lot info is required:
        for(int l = 0; l < messageList.size(); l++) {
            String message = messageList.get(l);
            if(message.length() > 0 && message.contains("%item-")) {
                if(auction != null) {
                    lot = auction.getLotType();
                    if(lot != null) {
                        replacements.put("%item-material-name%", this.translation.getTranslation(lot)); //%L1
                        replacements.put("%item-display-name%", Items.getDisplayName(lot)); //%L2
                        if(replacements.get("%item-display-name%") == null || replacements.get("%item-display-name%").isEmpty()) {
                            replacements.put("%item-display-name%", replacements.get("%item-material-name%"));
                        }
                        if(Items.getFireworkPower(lot) != null) {
                            replacements.put("%item-firework-power%", Integer.toString(Items.getFireworkPower(lot))); //%L3
                        }
                        if(Items.getBookAuthor(lot) != null) {
                            replacements.put("%item-book-author%", Items.getBookAuthor(lot)); //%L4
                        }
                        if(Items.getBookTitle(lot) != null) {
                            replacements.put("%item-book-title%", Items.getBookTitle(lot)); //%L5
                        }
                        if(lot.getType().getMaxDurability() > 0) {
                            DecimalFormat decimalFormat = new DecimalFormat("#%");
                            replacements.put("%item-durability-left%", decimalFormat.format((1 - ((double) lot.getDurability() / (double) lot.getType().getMaxDurability())))); //%L6
                        }
                        Map<Enchantment, Integer> enchantments = lot.getEnchantments();
                        if(enchantments == null || enchantments.size() == 0) {
                            enchantments = Items.getStoredEnchantments(lot);
                        }
                        if(enchantments != null) {
                            String enchantmentList = "";
                            String enchantmentSeparator = ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("auction-info-enchantment-separator", auctionScope));
                            for(Map.Entry<Enchantment, Integer> enchantment : enchantments.entrySet()) {
                                if(!enchantmentList.isEmpty()) enchantmentList += enchantmentSeparator;
                                enchantmentList += Items.getEnchantmentName(enchantment);
                            }
                            if(enchantmentList.isEmpty())
                                enchantmentList = ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("auction-info-enchantment-none", auctionScope));
                            replacements.put("%item-enchantments%", enchantmentList); //%L7
                        }
                    }
                }
                break;
            }
        }


        // Search to see if player info is required:
        for(int l = 0; l < messageList.size(); l++) {
            String message = messageList.get(l);
            if(message.length() > 0 && message.contains("%auction-prep")) {
                if(player != null) {
                    UUID playerUUID = player.getUniqueId();
                    String[] defaultStartArgs = Functions.mergeInputArgs(playerUUID, new String[]{}, false);
                    if(defaultStartArgs[0].equalsIgnoreCase("this") || defaultStartArgs[0].equalsIgnoreCase("hand")) {
                        replacements.put("%auction-prep-amount-other%", ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("prep-amount-in-hand", auctionScope))); //%P1
                    } else if(defaultStartArgs[0].equalsIgnoreCase("all")) {
                        replacements.put("%auction-prep-amount-other%", ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("prep-all-of-this-kind", auctionScope))); //%P1
                    } else {
                        replacements.put("%auction-prep-amount-other%", ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("prep-qty-of-this-kind", auctionScope))); //%P1
                    }

                    replacements.put("%auction-prep-amount-other%", defaultStartArgs[0]); //%P2
                    replacements.put("%auction-prep-price-formatted%", Functions.formatAmount(Double.parseDouble(defaultStartArgs[1]))); //%P3
                    replacements.put("%auction-prep-price%", defaultStartArgs[1]); //%P4
                    replacements.put("%auction-prep-increment-formatted%", Functions.formatAmount(Double.parseDouble(defaultStartArgs[2]))); //%P5
                    replacements.put("%auction-prep-increment%", defaultStartArgs[2]); //%P6
                    replacements.put("%auction-prep-time-formatted%", Functions.formatTime(Integer.parseInt(defaultStartArgs[3]), auctionScope)); //%P7
                    replacements.put("%auction-prep-time%", defaultStartArgs[3]); //%P8
                    replacements.put("%auction-prep-buynow-formatted%", Functions.formatAmount(Double.parseDouble(defaultStartArgs[4]))); //%P9
                    replacements.put("%auction-prep-buynow%", defaultStartArgs[4]); //%P0
                }
                break;
            }
        }

        // Search to see if scope info is required:
        for(int l = 0; l < messageList.size(); l++) {
            String message = messageList.get(l);
            if(message.length() > 0 && message.contains("%player-auction-queue") || message.contains("%auction-")) {
                if(auctionScope != null) {
                    if(player != null) {
                        replacements.put("%player-auction-queue-position%", Integer.toString(auctionScope.getQueuePosition(player.getName()))); //%S1
                    }
                    replacements.put("%auction-queue-length%", Integer.toString(auctionScope.getAuctionQueueLength())); //%S2
                    replacements.put("%auction-scope-name%", auctionScope.getName()); //%S3
                    replacements.put("%auction-scope-id%", auctionScope.getScopeId()); //%S4
                }
                break;
            }
        }

        // Search to see if conditionals are required:
        Map<String, Boolean> conditionals = new HashMap<>();
        for(int l = 0; l < messageList.size(); l++) {
            String message = messageList.get(l);
            if(message.length() > 0) {
                String lotTypeStr = null;
                if(lot != null) {
                    lotTypeStr = lot.getType().toString();
                }
                conditionals.put("is-admin", player != null && ObsidianAuctions.get().getPermission().has(player, "auction.admin")); //1
                conditionals.put("can-start", player != null && ObsidianAuctions.get().getPermission().has(player, "auction.start")); //2
                conditionals.put("can-bid", player != null && ObsidianAuctions.get().getPermission().has(player, "auction.bid")); //3
                conditionals.put("has-display-name", lot != null && ObsidianAuctions.allowRenamedItems && lot.getItemMeta() != null && lot.getItemMeta().hasDisplayName());
                conditionals.put("has-enchantment", lot != null && lot.getEnchantments() != null && lot.getEnchantments().size() > 0); //5
                conditionals.put("is-sealed", auction != null && auction.sealed); //6
                conditionals.put("not-sealed", auction != null && !auction.sealed && auction.getCurrentBid() != null); //7
                conditionals.put("is-broadcast", isBroadcast); //8
                conditionals.put("has-book-title", lot != null && Items.getBookTitle(lot) != null && !Items.getBookTitle(lot).isEmpty()); //9
                conditionals.put("has-book-author", lot != null && Items.getBookAuthor(lot) != null && !Items.getBookAuthor(lot).isEmpty()); //0
                conditionals.put("item-has-lore", lot != null && Items.getLore(lot) != null && Items.getLore(lot).length > 0); //A
                conditionals.put("has-durability", lot != null && lot.getType().getMaxDurability() > 0 && lot.getDurability() > 0); //B
                conditionals.put("is-firework", lot != null && (lotTypeStr.equals("FIREWORK")
                        || lotTypeStr.equals("FIREWORK_CHARGE") || lotTypeStr.equals("FIREWORK_ROCKET"))); //C
                conditionals.put("is-buynow", auction != null && auction.getBuyNow() != 0); //D
                conditionals.put("has-enchantments", lot != null && ((lot.getEnchantments() != null && lot.getEnchantments().size() > 0) || (Items.getStoredEnchantments(lot) != null && Items.getStoredEnchantments(lot).size() > 0))); //E
                conditionals.put("allow-max-bids", AuctionConfig.getBoolean("allow-max-bids", auctionScope)); //F
                conditionals.put("allow-buynow", AuctionConfig.getBoolean("allow-buynow", auctionScope)); //G
                conditionals.put("allow-auto-bid", AuctionConfig.getBoolean("allow-auto-bid", auctionScope)); //H
                conditionals.put("allow-early-bid", AuctionConfig.getBoolean("allow-early-end", auctionScope)); //I
                conditionals.put("cancel-prevention-percent", AuctionConfig.getInt("cancel-prevention-percent", auctionScope) < 100); //J
                conditionals.put("allow-unsealed-auctions", AuctionConfig.getBoolean("allow-unsealed-auctions", auctionScope)); //K
                conditionals.put("allow-sealed-auctions", AuctionConfig.getBoolean("allow-sealed-auctions", auctionScope)); //L
                conditionals.put("is-item-logic", conditionals.get("allow-unsealed-auctions") || conditionals.get("allow-sealed-auctions")); //L or K
                conditionals.put("get-active-auction", auctionScope != null && auctionScope.getActiveAuction() != null); //N
                conditionals.put("item-is-in-queue", auctionScope != null && auctionScope.getAuctionQueueLength() > 0); //O
                break;
            }
        }

        // Apply replacements and duplicate/remove rows that need it.
        for(int l = 0; l < messageList.size(); l++) {
            String message = ChatColor.translateAlternateColorCodes('&', messageList.get(l));

            if(message.length() > 0) {
                message = this.parseConditionals(message, conditionals);
            }
            if(message.length() == 0) { //If the length is 0 due to replacements just keep going
                continue;
            }

            // Make standard replacements.
            for(Map.Entry<String, String> replacementEntry : replacements.entrySet()) {
                message = message.replace(replacementEntry.getKey(), replacementEntry.getValue());
            }

            // Only one repeatable can be processed per line.
            if(message.contains("%repeatable")) {
                // Mental note: I'm not caching these because there is no reason to use them more than once per message.
                if(message.contains("%repeatable-enchantments%")) // Enchantments
                {
                    if(lot != null) {
                        // Stored enchantments and regular ones are treated identically.
                        Map<Enchantment, Integer> enchantments = lot.getEnchantments();
                        if(enchantments == null) {
                            enchantments = Items.getStoredEnchantments(lot);
                        } else {
                            Map<Enchantment, Integer> storedEnchantments = Items.getStoredEnchantments(lot);
                            if(storedEnchantments != null) {
                                enchantments.putAll(storedEnchantments);
                            }
                        }
                        if(enchantments != null && enchantments.size() > 0) {
                            for(Map.Entry<Enchantment, Integer> enchantmentEntry : enchantments.entrySet()) {
                                if(message.length() > 0) {
                                    newMessageList.add(chatPrep(message, auctionScope).replace("%repeatable-enchantment%", Items.getEnchantmentName(enchantmentEntry)));
                                }
                            }
                        }
                    }
                } else if(message.contains("%repeatable-firework-payload%")) { // Firework aspects
                    FireworkEffect[] payloads = Items.getFireworkEffects(lot);
                    if(payloads != null && payloads.length > 0) {
                        for(int j = 0; j < payloads.length; j++) {
                            FireworkEffect payload = payloads[j];
                            // %A lists all aspects of the payload

                            String payloadAspects = "";
                            String payloadSeparator = ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("auction-info-payload-separator", auctionScope));

                            FireworkEffect.Type type = payload.getType();
                            if(type != null) {
                                if(!payloadAspects.isEmpty()) payloadAspects += payloadSeparator;
                                String fireworkShape = AuctionConfig.getLanguageString("firework-shapes." + type.toString(), auctionScope);
                                if(fireworkShape == null) {
                                    payloadAspects += type.toString();
                                } else {
                                    payloadAspects += ChatColor.translateAlternateColorCodes('&', fireworkShape);
                                }
                            }
                            List<Color> colors = payload.getColors();
                            for(int k = 0; k < colors.size(); k++) {
                                if(!payloadAspects.isEmpty()) {
                                    payloadAspects += payloadSeparator;
                                }
                                Color color = colors.get(k);
                                String colorRGB = color.toString().replace("Color:[rgb0x", "").replace("]", "");
                                String fireworkColor = AuctionConfig.getLanguageString("firework-colors." + colorRGB, auctionScope);
                                if(fireworkColor == null) {
                                    payloadAspects += "#" + colorRGB;
                                } else {
                                    payloadAspects += ChatColor.translateAlternateColorCodes('&', fireworkColor);
                                }
                            }
                            if(payload.hasFlicker()) {
                                if(!payloadAspects.isEmpty()) {
                                    payloadAspects += payloadSeparator;
                                }

                                payloadAspects += ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("firework-twinkle", auctionScope));
                            }
                            if(payload.hasTrail()) {
                                if(!payloadAspects.isEmpty()) {
                                    payloadAspects += payloadSeparator;
                                }
                                payloadAspects += ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("firework-trail", auctionScope));
                            }
                            if(message.length() > 0) {
                                newMessageList.add(chatPrep(message, auctionScope).replace("%repeatable-firework-payload%", payloadAspects));
                            }
                        }
                        continue;
                    }
                } else if(message.contains("%repeatable-lore%")) {
                    if(auction != null) {
                        String[] lore = Items.getLore(lot);
                        for(int j = 0; j < lore.length; j++) {
                            if(message.length() > 0) {
                                newMessageList.add(chatPrep(message, auctionScope).replace("%repeatable-lore%", lore[j]));
                            }
                        }
                    }
                }
            } else {
                if(message.length() > 0) {
                    newMessageList.add(chatPrep(message, auctionScope));
                }
            }
        }
        if(newMessageList != null) {
            if(ObsidianAuctions.placeHolderApiEnabled) {
                for(int i = 0; i < newMessageList.size(); i++) {
                    newMessageList.set(i, PlaceholderAPIUtil.setPlaceHolders(player, newMessageList.get(i)));
                }
            }
        }
        return newMessageList;
    }

    public String parseConditionals(String message, Map<String, Boolean> conditionals) {
        String built = "";
        char[] chars = message.toCharArray();
        boolean open = false;
        boolean not = false;
        String inner = "";
        boolean copy = true;
        for(int i = 0; i < chars.length; i++) {
            char ch = chars[i];
            if(ch == '{' || ch == '}') { //Looking for a pattern
                if(open && ch == '}') {
                    open = false;
                    if(inner.equals("end")) { //If it is end we should just break out
                        inner = "";
                        if(!copy) {
                            return built;
                        }
                    } else if(inner.startsWith("end-")) { //The end of a conditional should match to the original
                        inner = "";
                        copy = true;
                        not = false;
                    } else { //If not any of those we will evaluate the inner text
                        Boolean eval = conditionals.get(inner);
                        if(eval != null) { //If the condition is not null, I.E a replacer we eval it
                            if(not) { //Check for not
                                eval = !eval;
                            }
                            copy = eval; //Inverse because if it is true we should not skip
                        } else { //If the condition was null we just append the whole thing to built
                            built += "{" + inner + "}";
                        }
                        inner = ""; //Clear the inner text
                        not = false;
                    }
                } else if(ch == '{') { //If it is the open bracket we should check start checking for the inner contents
                    open = true;
                }
            } else if(open) { //Check for open
                if(ch == '!') { //Check for not
                    not = !not;
                } else { //If open and not a modifier symbol start building the inner
                    inner += ch;
                }
            } else if(copy) {
                built += ch;
            }
        }
        return built;
    }

    /**
     * Prepares chat, prepending prefix and processing colors.
     *
     * @param message      message to prepare
     * @param auctionScope the scope of the destination
     * @return prepared message
     */
    private static String chatPrep(String message, AuctionScope auctionScope) {
        message = ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("chat-prefix", auctionScope)) + message;
        return message;
    }
}

package com.xwaffle.universalmarket.commands;

import com.xwaffle.universalmarket.UniversalMarket;
import com.xwaffle.universalmarket.market.MarketItem;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.type.HandType;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.math.BigDecimal;

/**
 * Created by Chase(Xwaffle) on 12/18/2017.
 */
public class MarketCommand extends BasicCommand {
    public MarketCommand() {
        super("", "Главная команда рынка.", "/market");
    }

    //start fix TheKlimoT
    public boolean isAir(ItemStack stack) {
        ItemType type = stack.getItem();
        return type.equals(ItemTypes.AIR);
    }
    //end fix TheKlimoT
    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        String[] args = arguments.split(" ");

        Player player = null;
        if (source instanceof Player) {
            player = (Player) source;
        }

        long expireTime = UniversalMarket.getInstance().getMarket().getExpireTime();
        long totalListings = UniversalMarket.getInstance().getMarket().getTotalItemsCanSell();


        if (arguments.isEmpty() || arguments.equalsIgnoreCase("")) {

            if (player != null) {
                if (player.hasPermission("com.xwaffle.universalmarket.open")) {
                    UniversalMarket.getInstance().getMarket().openMarket(player);
                } else {
                    source.sendMessage(Text.of(TextColors.RED, "У вас нет прав на открытие этого окна."));
                }
            } else {
                source.sendMessage(Text.of(TextColors.RED + "Маркте можно открыть только игроку!"));
            }
            return CommandResult.success();
        }

        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "open":
                case "o":
                    if (player == null)
                        break;
                    if (player.hasPermission("com.xwaffle.universalmarket.open")) {
                        UniversalMarket.getInstance().getMarket().openMarket(player);
                    } else {
                        player.sendMessage(Text.of(TextColors.RED, "У вас нет прав на открытие этого окна."));
                    }
                    break;
                case "add":
                case "a":
                    if (player == null)
                        break;
                    if (!player.hasPermission("com.xwaffle.universalmarket.add")) {
                        player.sendMessage(Text.of(TextColors.RED, "У вас нет прав на добавление товара в магазин."));
                        return CommandResult.success();
                    }

                    int listingCount = UniversalMarket.getInstance().getMarket().countListings(player.getUniqueId());
                    if (args.length < 2) {
                        player.sendMessage(Text.of(TextColors.RED, "Неверная команда!"));
                        player.sendMessage(Text.of(TextColors.YELLOW, "/um " + args[0].toLowerCase() + " (цена держа предмет в руке) (<опционально> количество)"));
                        return CommandResult.success();
                    }

                    if (listingCount >= totalListings) {
                        player.sendMessage(Text.of(TextColors.RED, "Вы уже продаете максимальное количество товаров за один раз."));
                        return CommandResult.success();
                    }


                    if (UniversalMarket.getInstance().getMarket().isUsePermissionToSell()) {
                        int userMaxSellPerm = 0;
                        for (int i = 1; i < 99; i++) {
                            if (player.hasPermission("com.xwaffle.universalmarket.addmax." + i)) {
                                userMaxSellPerm = i;
                            }
                        }


                        if (userMaxSellPerm <= listingCount) {
                            player.sendMessage(Text.of(TextColors.RED, "Вы достигли максимального количества предметов, которые вы можете продать на рынке."));
                            player.sendMessage(Text.of(TextColors.RED, "У вас есть разрешение на продажу только ", TextColors.GRAY, userMaxSellPerm, TextColors.RED, " предметов на рынке."));
                            return CommandResult.success();
                        }
                        //start fix TheKlimoT
                        if(isAir(player.getItemInHand(HandTypes.MAIN_HAND).orElse(ItemStack.empty()))){
                            player.sendMessage(Text.of(TextColors.RED, "У вас пустая рука."));
                            return CommandResult.success();
                        }
                        //end fix TheKlimoT
                    }


                    if (player.getItemInHand(HandTypes.MAIN_HAND).isPresent()) {
                        ItemStack stack = player.getItemInHand(HandTypes.MAIN_HAND).get();
                        double price;
                        try {
                            price = Double.parseDouble(args[1]);

                            if (price < 0) {
                                player.sendMessage(Text.of(TextColors.RED, "Вы ввели неккоректную цену."));
                                return CommandResult.success();

                            }
                        } catch (Exception exc) {
                            player.sendMessage(Text.of(TextColors.RED, "Недопустимая сумма для товара!"));
                            player.sendMessage(Text.of(TextColors.YELLOW, "/um " + args[0].toLowerCase() + " (цена держа в руке) (<опционально> количество)"));
                            return CommandResult.success();
                        }

                        int amount = stack.getQuantity();

                        if (args.length >= 3) {
                            try {
                                amount = Integer.parseInt(args[2]);
                                if (amount <= 0) {
                                    player.sendMessage(Text.of(TextColors.RED, "Вы должны ввести положительное число для продажи на рынке!"));
                                    return CommandResult.success();
                                } else if (amount > stack.getQuantity()) {
                                    player.sendMessage(Text.of(TextColors.RED, "Нельзя продать больше чем у вас есть!"));
                                    return CommandResult.success();
                                }
                            } catch (Exception exc) {
                                player.sendMessage(Text.of(TextColors.RED, "Недопустимая сумма для товара!"));
                                player.sendMessage(Text.of(TextColors.YELLOW, "/um " + args[0].toLowerCase() + " (цена держа в руке) (<опционально> количество)"));
                                return CommandResult.success();
                            }
                        }

                        if (UniversalMarket.getInstance().getMarket().useTax()) {
                            double tax = price * UniversalMarket.getInstance().getMarket().getTax();

                            if (UniversalMarket.getInstance().getEconomyService() == null) {
                                source.sendMessage(Text.of(TextColors.RED, "This server is not using a currency plugin! This is required to use Universal Market!"));
                                return CommandResult.success();
                            }

                            UniqueAccount account = UniversalMarket.getInstance().getEconomyService().getOrCreateAccount(player.getUniqueId()).get();
                            Currency currency = UniversalMarket.getInstance().getEconomyService().getDefaultCurrency();
                            if (account.getBalance(currency).doubleValue() < tax) {
                                player.sendMessage(Text.of(TextColors.RED, "Вам не хватает денег для уплаты налога за выставление товара на продажу!"));
                                player.sendMessage(Text.of(TextColors.RED, "Вы должны заплатить ", TextColors.YELLOW, UniversalMarket.getInstance().getMarket().getTax(), TextColors.RED, " от цены товара."));
                                player.sendMessage(Text.of(TextColors.RED, "Вы должны заплатить ", TextColors.GREEN, tax, TextColors.RED, " для продажи этого товара на рынке."));
                                return CommandResult.success();
                            } else {
                                account.withdraw(currency, new BigDecimal(tax), Cause.of(EventContext.empty(), UniversalMarket.getInstance()));
                                player.sendMessage(Text.of(TextColors.RED, "Вы заплатили налог за выставление товара на продажу!"));
                                player.sendMessage(Text.of(TextColors.DARK_RED, "- $", TextColors.RED, tax));
                            }
                        }

                        if (UniversalMarket.getInstance().getMarket().payFlatPrice()) {
                            double flatPrice = UniversalMarket.getInstance().getMarket().getFlatPrice();
                            UniqueAccount account = UniversalMarket.getInstance().getEconomyService().getOrCreateAccount(player.getUniqueId()).get();
                            Currency currency = UniversalMarket.getInstance().getEconomyService().getDefaultCurrency();
                            if (account.getBalance(currency).doubleValue() < flatPrice) {
                                player.sendMessage(Text.of(TextColors.RED, "Вы должны заплатить ", TextColors.GRAY, "$" + flatPrice, TextColors.RED, " для продажи на рынке."));
                                return CommandResult.success();
                            } else {
                                account.withdraw(currency, new BigDecimal(flatPrice), Cause.of(EventContext.empty(), UniversalMarket.getInstance()));
                                player.sendMessage(Text.of(TextColors.RED, "Была взята рыночная плата!"));
                                player.sendMessage(Text.of(TextColors.DARK_RED, "- $", TextColors.RED, flatPrice));
                            }
                        }

                        if (UniversalMarket.getInstance().getMarket().isItemBlacklisted(stack.getType())) {
                            player.sendMessage(Text.of(TextColors.RED, "Этот товар не может быть продан (" + stack.getType().getId() + ")"));
                            return CommandResult.success();
                        }


                        int prevAmount = stack.getQuantity();

                        if (amount == stack.getQuantity()) {
                            player.setItemInHand(HandTypes.MAIN_HAND, null);
                        } else {

                            stack.setQuantity(amount);
                        }


                        int id = UniversalMarket.getInstance().getDatabase().createEntry(stack.copy(), player.getUniqueId(), player.getName(), price, System.currentTimeMillis() + expireTime);
                        UniversalMarket.getInstance().getMarket().addItem(new MarketItem(id, stack.copy(), player.getUniqueId(), player.getName(), price, (System.currentTimeMillis() + expireTime)), false);
                        player.sendMessage(Text.of(TextColors.YELLOW, "Предмет добавлен в продажу", TextColors.YELLOW, " за $", TextColors.DARK_AQUA, price));

                        if (amount != prevAmount) {
                            stack.setQuantity(prevAmount - amount);
                            player.setItemInHand(HandTypes.MAIN_HAND, stack);
                        }


                    } else {
                        player.sendMessage(Text.of(TextColors.RED, "Поместите предмет в вашу руку, чтобы продать!"));
                    }
                    break;
                case "help":
                case "h":
                case "?":
                    source.sendMessage(Text.of(TextColors.DARK_AQUA, "Магазин предметов | Подсказки"));
                    source.sendMessage(Text.of(TextColors.YELLOW, "/um или /universalmarket"));
                    source.sendMessage(Text.of(TextColors.YELLOW, "/um a (цена) (<опционально> amount) или /um add (цена) (<опционально> amount)", TextColors.GRAY, " - ", TextColors.GREEN, "Продажа предмета, держите предмет в руке."));
                    source.sendMessage(Text.of(TextColors.YELLOW, "/um o или /um open", TextColors.GRAY, " - ", TextColors.GREEN, "Открыть магазин предметов."));
                    source.sendMessage(Text.of(TextColors.YELLOW, "/um i или /um info", TextColors.GRAY, " - ", TextColors.GREEN, "Отобразить настройки магазина."));
                    source.sendMessage(Text.of(TextColors.YELLOW, "/um r или /um reload", TextColors.GRAY, " - ", TextColors.GREEN, "Перезагрузка конфига."));
                    break;
                case "reload":
                case "r":
                    if (source.hasPermission("com.xwaffle.universalmarket.reload")) {
                        UniversalMarket.getInstance().getMarket().reloadConfig();
                        source.sendMessage(Text.of(TextColors.GREEN, "Конфиг маркета перезагружен!"));
                    } else {
                        source.sendMessage(Text.of(TextColors.RED, "У вас нет прав на это!"));
                    }
                    break;
                case "info":
                case "i":
                    source.sendMessage(Text.of(TextColors.DARK_AQUA, "Текущий налоговый процент: ", TextColors.AQUA, UniversalMarket.getInstance().getMarket().getTax()));
                    break;
            }
        } else {
            if (player != null) {
                UniversalMarket.getInstance().getMarket().openMarket(player);
            }
        }

        return CommandResult.success();
    }
}

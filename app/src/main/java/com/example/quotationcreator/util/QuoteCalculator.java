package com.example.quotationcreator.util;

import com.example.quotationcreator.model.Item;
import com.example.quotationcreator.model.Quote;

import java.util.List;

public final class QuoteCalculator {

    private QuoteCalculator() {
    }

    public static Summary calculate(Quote quote) {
        List<Item> items = quote.getItems();

        double subtotal = 0d;
        double discountTotal = 0d;

        for (Item item : items) {
            double baseAmount = lineBaseAmount(item);
            double lineNetAmount = Math.max(0d, baseAmount);

            subtotal += baseAmount;
            item.setLineAmount(lineNetAmount);
        }

        double taxableAmount = Math.max(0d, subtotal);
        double taxAmount = quote.isTaxEnabled() ? taxableAmount * (quote.getTaxPercent() / 100d) : 0d;
        double vatAmount = quote.isVatEnabled() ? taxableAmount * (quote.getVatPercent() / 100d) : 0d;

        double grandBeforeRound = taxableAmount + taxAmount + vatAmount;

        double roundOffDelta = 0d;
        double finalTotal = grandBeforeRound;

        if (quote.isRoundOffEnabled()) {
            double rounded = Math.rint(grandBeforeRound);
            roundOffDelta = rounded - grandBeforeRound;
            finalTotal = rounded;
        }

        return new Summary(
                subtotal,
                discountTotal,
                taxableAmount,
                taxAmount,
                vatAmount,
                grandBeforeRound,
                roundOffDelta,
                finalTotal
        );
    }

    public static double lineBaseAmount(Item item) {
        return Math.max(0d, item.getQuantity()) * Math.max(0d, item.getUnitPrice());
    }

    public static double lineDiscount(Item item) {
        return 0d;
    }

    public static double lineNetAmount(Item item) {
        return Math.max(0d, lineBaseAmount(item));
    }

    public static final class Summary {

        private final double subtotal;
        private final double discountTotal;
        private final double taxableAmount;
        private final double taxAmount;
        private final double vatAmount;
        private final double grandBeforeRound;
        private final double roundOffDelta;
        private final double finalTotal;

        public Summary(
                double subtotal,
                double discountTotal,
                double taxableAmount,
                double taxAmount,
                double vatAmount,
                double grandBeforeRound,
                double roundOffDelta,
                double finalTotal
        ) {
            this.subtotal = subtotal;
            this.discountTotal = discountTotal;
            this.taxableAmount = taxableAmount;
            this.taxAmount = taxAmount;
            this.vatAmount = vatAmount;
            this.grandBeforeRound = grandBeforeRound;
            this.roundOffDelta = roundOffDelta;
            this.finalTotal = finalTotal;
        }

        public double getSubtotal() {
            return subtotal;
        }

        public double getDiscountTotal() {
            return discountTotal;
        }

        public double getTaxableAmount() {
            return taxableAmount;
        }

        public double getTaxAmount() {
            return taxAmount;
        }

        public double getVatAmount() {
            return vatAmount;
        }

        public double getGrandBeforeRound() {
            return grandBeforeRound;
        }

        public double getRoundOffDelta() {
            return roundOffDelta;
        }

        public double getFinalTotal() {
            return finalTotal;
        }
    }
}

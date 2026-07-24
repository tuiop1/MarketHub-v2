package dev.tuiop.notificationservice.email;

import dev.tuiop.commonevents.CustomerRegisteredEvent;
import dev.tuiop.commonevents.MerchantRegisteredEvent;
import dev.tuiop.commonevents.OrderConfirmedEvent;
import dev.tuiop.commonevents.OrderConfirmedItemSnapshot;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.util.stream.Collectors;

@Component
public class EmailTemplateFactory {

    public String customerRegistered(CustomerRegisteredEvent event) {
        return """
                <h2>Welcome to MarketHub, %s</h2>
                <p>Your customer account has been created successfully.</p>
                <p>You can now browse products and place orders.</p>
                """.formatted(escape(event.firstName()));
    }

    public String merchantRegistered(MerchantRegisteredEvent event) {
        return """
                <h2>Welcome to MarketHub</h2>
                <p>Your merchant account for <strong>%s</strong> has been created.</p>
                <p>Your account is currently pending verification.</p>
                """.formatted(escape(event.shopName()));
    }

    public String orderPaid(OrderConfirmedEvent event) {
        String rows = event.items()
                .stream()
                .map(this::orderItemRow)
                .collect(Collectors.joining());

        return """
                <h2>Thanks for your order, %s</h2>
                <p>Your order <strong>%s</strong> has been paid successfully.</p>
                
                <table border="1" cellpadding="8" cellspacing="0">
                    <thead>
                        <tr>
                            <th>Product</th>
                            <th>Quantity</th>
                            <th>Unit price</th>
                            <th>Total</th>
                        </tr>
                    </thead>
                    <tbody>
                        %s
                    </tbody>
                </table>
                
                <p><strong>Total: %s</strong></p>
                """.formatted(
                escape(event.customerFirstName()),
                event.orderId(),
                rows,
                money(event.totalPriceCents() ,"EUR")
        );
    }

    private String orderItemRow(OrderConfirmedItemSnapshot item) {
        return """
                <tr>
                    <td>%s</td>
                    <td>%d</td>
                    <td>%s</td>
                    <td>%s</td>
                </tr>
                """.formatted(
                escape(item.productName()),
                item.quantity(),
                money(item.unitPriceCents(), "EUR"),
                money(item.totalPriceCents(), "EUR")
        );
    }

    private String money(Long cents, String currency) {
        return BigDecimal.valueOf(cents, 2).toPlainString()
                + " "
                + currency;
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }
}

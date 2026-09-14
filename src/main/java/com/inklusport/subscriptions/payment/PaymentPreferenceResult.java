package com.inklusport.subscriptions.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentPreferenceResult {
    private String preferenceId;
    private String checkoutUrl;
}

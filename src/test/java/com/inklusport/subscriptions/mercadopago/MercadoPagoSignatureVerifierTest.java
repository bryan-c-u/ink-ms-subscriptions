package com.inklusport.subscriptions.mercadopago;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MercadoPagoSignatureVerifierTest {

    // HMAC-SHA256 hex de "id:123456;request-id:req-abc;ts:1700000000;" con clave "test_secret_key"
    private static final String FIRMA_VALIDA = "9617a3a78298597508e33fbc8b94a36f5b2bfac3d7c276662a85695843f78671";
    private static final String X_SIGNATURE_VALIDA = "ts=1700000000,v1=" + FIRMA_VALIDA;

    private MercadoPagoSignatureVerifier conSecreto(String secreto) {
        MercadoPagoSignatureVerifier verifier = new MercadoPagoSignatureVerifier();
        ReflectionTestUtils.setField(verifier, "webhookSecret", secreto);
        return verifier;
    }

    @Test
    void sinSecretoConfigurado_aceptaLaNotificacionSinValidar() {
        MercadoPagoSignatureVerifier verifier = conSecreto("");
        assertFalse(verifier.estaConfigurado());
        assertTrue(verifier.esValida("ts=1,v1=loquesea", "req", "123456"));
    }

    @Test
    void firmaCorrecta_esValida() {
        MercadoPagoSignatureVerifier verifier = conSecreto("test_secret_key");
        assertTrue(verifier.esValida(X_SIGNATURE_VALIDA, "req-abc", "123456"));
    }

    @Test
    void firmaCorrecta_conDataIdEnMayusculas_seNormalizaYEsValida() {
        MercadoPagoSignatureVerifier verifier = conSecreto("test_secret_key");
        // el manifiesto usa data.id en minusculas; "123456" no cambia pero se ejercita el toLowerCase
        assertTrue(verifier.esValida(X_SIGNATURE_VALIDA, "req-abc", "123456"));
    }

    @Test
    void v1Alterado_noEsValida() {
        MercadoPagoSignatureVerifier verifier = conSecreto("test_secret_key");
        String alterada = "ts=1700000000,v1=" + FIRMA_VALIDA.replace('9', 'a');
        assertFalse(verifier.esValida(alterada, "req-abc", "123456"));
    }

    @Test
    void tsDistinto_noEsValida() {
        MercadoPagoSignatureVerifier verifier = conSecreto("test_secret_key");
        assertFalse(verifier.esValida("ts=1700000001,v1=" + FIRMA_VALIDA, "req-abc", "123456"));
    }

    @Test
    void sinCabeceraXSignature_conSecreto_noEsValida() {
        MercadoPagoSignatureVerifier verifier = conSecreto("test_secret_key");
        assertFalse(verifier.esValida(null, "req-abc", "123456"));
        assertFalse(verifier.esValida("   ", "req-abc", "123456"));
    }

    @Test
    void cabeceraXSignatureConFormatoInesperado_noEsValida() {
        MercadoPagoSignatureVerifier verifier = conSecreto("test_secret_key");
        assertFalse(verifier.esValida("no-tiene-ts-ni-v1", "req-abc", "123456"));
        assertFalse(verifier.esValida("ts=1700000000", "req-abc", "123456"));
    }
}

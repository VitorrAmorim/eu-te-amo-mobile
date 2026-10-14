package com.example.eu_te_amo_mobile

import com.example.eu_te_amo_mobile.dados.FRASE_CENTRAL
import com.example.eu_te_amo_mobile.dados.LISTA_FRASES
import com.example.eu_te_amo_mobile.dominio.CAIXA_DELIMITADORA
import com.example.eu_te_amo_mobile.dominio.calcularLayoutDeFrases
import com.example.eu_te_amo_mobile.dominio.estaDentroDoCoracao
import com.example.eu_te_amo_mobile.ui.DURACAO_ANIMACAO_MS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

class CoracaoLayoutTest {

    @Test
    fun testGeometriaCoracao() {
        assertTrue(estaDentroDoCoracao(0.0, 0.0))
        assertTrue(estaDentroDoCoracao(0.0, -0.99))
        assertTrue(estaDentroDoCoracao(0.0, 0.99))
        assertFalse(estaDentroDoCoracao(2.0, 2.0))
        assertFalse(estaDentroDoCoracao(-2.0, -2.0))

        assertTrue(CAIXA_DELIMITADORA.minX < 0.0)
        assertTrue(CAIXA_DELIMITADORA.maxX > 0.0)
        assertTrue(CAIXA_DELIMITADORA.minY < 0.0)
        assertTrue(CAIXA_DELIMITADORA.maxY > 0.0)
    }

    @Test
    fun testConstantesAnimacao() {
        assertEquals(8000L, DURACAO_ANIMACAO_MS)
    }

    @Test
    fun testCalculoLayout() {
        val larguraTela = 1080f
        val alturaTela = 2400f
        val densidade = 2.75f

        val medirLargura: (String, Float) -> Float = { texto, tamanhoPx ->
            texto.length * tamanhoPx * 0.55f
        }

        var resultado = emptyList<com.example.eu_te_amo_mobile.dominio.FraseColocada>()
        val tempoMs = measureTimeMillis {
            resultado = calcularLayoutDeFrases(
                larguraTela = larguraTela,
                alturaTela = alturaTela,
                listaFrases = LISTA_FRASES,
                medirLargura = medirLargura,
                semente = 42L,
                densidade = densidade,
            )
        }

        println("Total de frases colocadas: ${resultado.size}")
        println("Tempo de cálculo do layout: ${tempoMs}ms")

        val fraseCentral = resultado.firstOrNull { it.ehCentral }
        assertTrue("A frase central deve estar presente", fraseCentral != null)
        assertEquals(FRASE_CENTRAL, fraseCentral?.texto)

        // Verifica que todas as frases estão dentro dos limites da tela
        for (frase in resultado) {
            val larguraFrase = medirLargura(frase.texto, frase.tamanhoPx)
            assertTrue("X inicial dentro da tela", frase.x >= 0f)
            assertTrue("X final dentro da tela", frase.x + larguraFrase <= larguraTela)
            assertTrue("Y inicial dentro da tela", frase.y >= 0f)
            assertTrue("Y final dentro da tela", frase.y + frase.tamanhoPx <= alturaTela)
        }
    }
}

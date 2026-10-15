package com.example.eu_te_amo_mobile

import com.example.eu_te_amo_mobile.dados.FRASE_CENTRAL
import com.example.eu_te_amo_mobile.dados.LISTA_FRASES
import com.example.eu_te_amo_mobile.dominio.CAIXA_DELIMITADORA
import com.example.eu_te_amo_mobile.dominio.MapeadorCoracao
import com.example.eu_te_amo_mobile.dominio.calcularLayoutDeFrases
import com.example.eu_te_amo_mobile.dominio.estaDentroDoCoracao
import com.example.eu_te_amo_mobile.ui.DURACAO_ANIMACAO_MS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
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
    fun testMetasNovoLayout() {
        val larguraTela = 1080f
        val alturaTela = 2400f
        val densidade = 2.75f

        val medirLargura: (String, Float) -> Float = { texto, tamanhoPx ->
            texto.length * tamanhoPx * 0.55f
        }

        val mapeador = MapeadorCoracao(larguraTela, alturaTela)
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

        println("Novo tempo de cálculo do layout: ${tempoMs}ms")
        println("Total de frases colocadas: ${resultado.size}")

        val central = resultado.first { it.ehCentral }
        assertEquals(FRASE_CENTRAL, central.texto)

        // Limites
        for (frase in resultado) {
            val larg = medirLargura(frase.texto, frase.tamanhoPx)
            assertTrue("X inicial dentro da tela", frase.x >= 0f)
            assertTrue("X final dentro da tela", frase.x + larg <= larguraTela)
            assertTrue("Y inicial dentro da tela", frase.y >= 0f)
            assertTrue("Y final dentro da tela", frase.y + frase.tamanhoPx <= alturaTela)
        }

        val minSpPx = 7f * densidade
        val t3 = max(0.034f * mapeador.larguraCoracaoPx, minSpPx)
        val hCentral = central.tamanhoPx
        val margemVert = t3 * 1.25f * 0.3f
        val yMinZona = central.y - margemVert
        val yMaxZona = central.y + hCentral + margemVert

        val frasesNaZona = resultado.filter { !it.ehCentral && (it.y + it.tamanhoPx >= yMinZona && it.y <= yMaxZona) }
        val frasesEsq = frasesNaZona.filter { it.x + medirLargura(it.texto, it.tamanhoPx) <= central.x }
        val frasesDir = frasesNaZona.filter { it.x >= central.x + medirLargura(central.texto, central.tamanhoPx) }

        println("Frases encontradas na zona central -> Esquerda: ${frasesEsq.size}, Direita: ${frasesDir.size}")
        assertTrue("Deve haver frases na esquerda da zona central", frasesEsq.isNotEmpty())
        assertTrue("Deve haver frases na direita da zona central", frasesDir.isNotEmpty())
    }
}

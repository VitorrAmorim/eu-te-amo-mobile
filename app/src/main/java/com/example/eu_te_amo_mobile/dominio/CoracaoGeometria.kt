package com.example.eu_te_amo_mobile.dominio

import kotlin.math.min

data class CaixaDelimitadora(
    val minX: Double,
    val maxX: Double,
    val minY: Double,
    val maxY: Double,
) {
    val largura: Double get() = maxX - minX
    val altura: Double get() = maxY - minY
    val centroX: Double get() = (minX + maxX) / 2.0
    val centroY: Double get() = (minY + maxY) / 2.0
}

fun estaDentroDoCoracao(x: Double, y: Double): Boolean {
    val termoBase = ((x * x) + (y * y)) - 1.0
    return ((termoBase * termoBase * termoBase) - (x * x * y * y * y)) <= 0.0
}

fun calcularCaixaDelimitadora(passo: Double = 0.001): CaixaDelimitadora {
    var minX = Double.MAX_VALUE
    var maxX = -Double.MAX_VALUE
    var minY = Double.MAX_VALUE
    var maxY = -Double.MAX_VALUE

    var y = -2.0
    while (y <= 2.0) {
        var x = -2.0
        while (x <= 2.0) {
            if (estaDentroDoCoracao(x, y)) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
            x += passo
        }
        y += passo
    }
    return CaixaDelimitadora(minX, maxX, minY, maxY)
}

val CAIXA_DELIMITADORA = calcularCaixaDelimitadora()

class MapeadorCoracao(
    larguraTela: Float,
    alturaTela: Float,
    margemPercentual: Float = 0.06f,
    val caixa: CaixaDelimitadora = CAIXA_DELIMITADORA,
) {
    val larguraDisponivel: Float = larguraTela * (1f - (2f * margemPercentual))
    val alturaDisponivel: Float = alturaTela * (1f - (2f * margemPercentual))

    val escala: Float = min(
        larguraDisponivel / caixa.largura.toFloat(),
        alturaDisponivel / caixa.altura.toFloat(),
    )

    val centroTelaX: Float = larguraTela / 2f
    val centroTelaY: Float = alturaTela / 2f

    val larguraCoracaoPx: Float = caixa.largura.toFloat() * escala
    val alturaCoracaoPx: Float = caixa.altura.toFloat() * escala

    @Suppress("unused")
    fun matParaTelaX(xMat: Double): Float =
        centroTelaX + ((xMat - caixa.centroX) * escala).toFloat()

    fun matParaTelaY(yMat: Double): Float =
        centroTelaY - ((yMat - caixa.centroY) * escala).toFloat()

    fun telaParaMatX(xTela: Float): Double =
        caixa.centroX + ((xTela - centroTelaX) / escala)

    fun telaParaMatY(yTela: Float): Double =
        caixa.centroY - ((yTela - centroTelaY) / escala)

    fun pontoTelaEstaDentro(xTela: Float, yTela: Float): Boolean {
        return estaDentroDoCoracao(telaParaMatX(xTela), telaParaMatY(yTela))
    }
}

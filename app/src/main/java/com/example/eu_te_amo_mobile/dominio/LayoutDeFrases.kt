package com.example.eu_te_amo_mobile.dominio

import com.example.eu_te_amo_mobile.dados.FRASE_CENTRAL
import com.example.eu_te_amo_mobile.dados.Frase
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

data class FraseColocada(
    val texto: String,
    val x: Float,
    val y: Float,
    val tamanhoPx: Float,
    val distanciaDoCentro: Float,
    val ehCentral: Boolean,
)

data class Retangulo(
    val xMin: Float,
    val yMin: Float,
    val xMax: Float,
    val yMax: Float,
)

fun calcularLayoutDeFrases(
    larguraTela: Float,
    alturaTela: Float,
    listaFrases: List<Frase>,
    medirLargura: (texto: String, tamanhoPx: Float) -> Float,
    semente: Long = 42L,
    densidade: Float = 1f,
): List<FraseColocada> {
    if ((larguraTela <= 0f) || (alturaTela <= 0f) || listaFrases.isEmpty()) {
        return emptyList()
    }

    val resultado = mutableListOf<FraseColocada>()
    val mapeador = MapeadorCoracao(larguraTela, alturaTela)
    val larguraCoracaoPx = mapeador.larguraCoracaoPx

    val yTopCoracao = mapeador.matParaTelaY(mapeador.caixa.maxY)
    val yBottomCoracao = mapeador.matParaTelaY(mapeador.caixa.minY)
    val alturaCoracaoPx = max(1f, yBottomCoracao - yTopCoracao)

    val minSpPx = 7f * densidade
    val t1 = max(0.024f * larguraCoracaoPx, minSpPx)
    val t2 = max(0.029f * larguraCoracaoPx, minSpPx)
    val t3 = max(0.034f * larguraCoracaoPx, minSpPx)
    val tamanhosPadrao = listOf(t3, t2, t1)

    val tDegrau1 = max(t1 * 0.85f, minSpPx)
    val tDegrau2 = max(t1 * 0.70f, minSpPx)
    val todosTamanhosPossiveis = listOf(t3, t2, t1, tDegrau1, tDegrau2).distinct().sortedDescending()

    val y40 = yTopCoracao + (0.40f * alturaCoracaoPx)
    val y60 = yTopCoracao + (0.60f * alturaCoracaoPx)

    var melhorYCentral = yTopCoracao + (0.50f * alturaCoracaoPx)
    var maiorLarguraCentralDisponivel = -1f

    var yScan = y40
    while (yScan <= y60) {
        var xEsq = mapeador.centroTelaX
        while ((xEsq > 0f) && mapeador.pontoTelaEstaDentro(xEsq, yScan)) xEsq -= 1f
        var xDir = mapeador.centroTelaX
        while ((xDir < larguraTela) && mapeador.pontoTelaEstaDentro(xDir, yScan)) xDir += 1f
        val larg = max(0f, xDir - xEsq)
        if (larg > maiorLarguraCentralDisponivel) {
            maiorLarguraCentralDisponivel = larg
            melhorYCentral = yScan
        }
        yScan += 2f
    }

    val centroX = mapeador.centroTelaX
    val centroY = melhorYCentral
    val larguraNaAlturaCentral = maiorLarguraCentralDisponivel

    val larguraMaxCentral = 0.42f * larguraNaAlturaCentral
    val tamanhoMinCentral = 2.5f * t3

    var tamanhoCentralPx = larguraMaxCentral
    var wCentral: Float
    var hCentral: Float

    while (tamanhoCentralPx > tamanhoMinCentral) {
        wCentral = medirLargura(FRASE_CENTRAL, tamanhoCentralPx)
        hCentral = tamanhoCentralPx
        val xCentralTmp = centroX - (wCentral / 2f)
        val yCentralTmp = centroY - (hCentral / 2f)

        if ((wCentral <= larguraMaxCentral) &&
            mapeador.pontoTelaEstaDentro(xCentralTmp, yCentralTmp) &&
            mapeador.pontoTelaEstaDentro(xCentralTmp + wCentral, yCentralTmp) &&
            mapeador.pontoTelaEstaDentro(xCentralTmp, yCentralTmp + hCentral) &&
            mapeador.pontoTelaEstaDentro(xCentralTmp + wCentral, yCentralTmp + hCentral)
        ) {
            break
        }
        tamanhoCentralPx -= 1f
    }
    tamanhoCentralPx = max(tamanhoCentralPx, tamanhoMinCentral)
    wCentral = medirLargura(FRASE_CENTRAL, tamanhoCentralPx)
    hCentral = tamanhoCentralPx

    val xCentral = centroX - (wCentral / 2f)
    val yCentral = centroY - (hCentral / 2f)

    resultado.add(
        FraseColocada(
            texto = FRASE_CENTRAL,
            x = xCentral,
            y = yCentral,
            tamanhoPx = tamanhoCentralPx,
            distanciaDoCentro = 0f,
            ehCentral = true,
        ),
    )

    val margemHoriz = t3 * 1.0f
    val alturaLinha = t3 * 1.25f
    val margemVert = alturaLinha * 0.3f

    val zonaReservada = Retangulo(
        xMin = xCentral - margemHoriz,
        yMin = yCentral - margemVert,
        xMax = xCentral + wCentral + margemHoriz,
        yMax = yCentral + hCentral + margemVert,
    )

    val yInicio = yTopCoracao
    val yFim = yBottomCoracao

    val rng = Random(semente)
    var ultimaFraseColocada: Frase? = null

    fun criarBaralho(): MutableList<Frase> {
        val embaralhado = listaFrases.shuffled(rng).toMutableList()
        if (ultimaFraseColocada != null && embaralhado.isNotEmpty() &&
            embaralhado.first() == ultimaFraseColocada && embaralhado.size > 1
        ) {
            val temp = embaralhado[0]
            embaralhado[0] = embaralhado[1]
            embaralhado[1] = temp
        }
        return embaralhado
    }

    var baralhoAtual = criarBaralho()

    fun renovarBaralhoSeNecessario() {
        if (baralhoAtual.isEmpty()) {
            baralhoAtual = criarBaralho()
        }
    }

    val margemInterna = larguraCoracaoPx * 0.015f
    val dMax = max(mapeador.larguraCoracaoPx, mapeador.alturaCoracaoPx) / 2f

    var yLinha = yInicio
    while (yLinha <= yFim - alturaLinha) {
        val yTop = yLinha
        val yBottom = yLinha + alturaLinha

        val trechosLinha = mutableListOf<Pair<Float, Float>>()
        var xStart: Float? = null

        var x = 0f
        while (x <= larguraTela) {
            val dentro = mapeador.pontoTelaEstaDentro(x, yTop) && mapeador.pontoTelaEstaDentro(x, yBottom)
            if (dentro) {
                if (xStart == null) xStart = x
            } else {
                if (xStart != null) {
                    val xEnd = x
                    val xStartMargem = xStart + margemInterna
                    val xEndMargem = xEnd - margemInterna
                    if (xEndMargem > xStartMargem) {
                        trechosLinha.add(Pair(xStartMargem, xEndMargem))
                    }
                    xStart = null
                }
            }
            x += 2f
        }
        if (xStart != null) {
            val xStartMargem = xStart + margemInterna
            val xEndMargem = larguraTela - margemInterna
            if (xEndMargem > xStartMargem) {
                trechosLinha.add(Pair(xStartMargem, xEndMargem))
            }
        }

        val trechosFinais = mutableListOf<Pair<Float, Float>>()
        val linhaCruzaZonaVert = yBottom >= zonaReservada.yMin && yTop <= zonaReservada.yMax

        for (trecho in trechosLinha) {
            if (linhaCruzaZonaVert) {
                val xStartT = trecho.first
                val xEndT = trecho.second

                val xEndEsq = min(xEndT, zonaReservada.xMin)
                if (xEndEsq > xStartT) {
                    trechosFinais.add(Pair(xStartT, xEndEsq))
                }

                val xStartDir = max(xStartT, zonaReservada.xMax)
                if (xEndT > xStartDir) {
                    trechosFinais.add(Pair(xStartDir, xEndT))
                }
            } else {
                trechosFinais.add(trecho)
            }
        }

        for (trecho in trechosFinais) {
            val xInicioT = trecho.first
            val xFimT = trecho.second
            val larguraDisponivel = xFimT - xInicioT

            val candidatos = mutableListOf<Triple<Frase, Float, Float>>() // (Frase, tamanhoPx, larguraMedida)
            var somaLarguras = 0f

            while (true) {
                renovarBaralhoSeNecessario()
                val espacoMinimo = 4f * densidade
                val espacoLivre = if (candidatos.isEmpty()) larguraDisponivel else larguraDisponivel - somaLarguras - (candidatos.size * espacoMinimo)

                if (espacoLivre <= 0f) break

                var melhorEscolha: Triple<Frase, Float, Float>? = null
                var indiceMelhorFrase = -1

                for ((idx, frase) in baralhoAtual.withIndex()) {
                    for (tam in tamanhosPadrao) {
                        val larg = medirLargura(frase.texto, tam)
                        if (larg <= espacoLivre) {
                            if (melhorEscolha == null || larg > melhorEscolha.third) {
                                melhorEscolha = Triple(frase, tam, larg)
                                indiceMelhorFrase = idx
                            }
                        }
                    }
                    if (melhorEscolha != null) break
                }

                if (melhorEscolha == null) {
                    for ((idx, frase) in baralhoAtual.withIndex()) {
                        for (tam in todosTamanhosPossiveis) {
                            val larg = medirLargura(frase.texto, tam)
                            if (larg <= espacoLivre) {
                                if (melhorEscolha == null || larg > melhorEscolha.third) {
                                    melhorEscolha = Triple(frase, tam, larg)
                                    indiceMelhorFrase = idx
                                }
                            }
                        }
                        if (melhorEscolha != null) break
                    }
                }

                if (melhorEscolha != null && indiceMelhorFrase >= 0) {
                    val fraseEscolhida = baralhoAtual.removeAt(indiceMelhorFrase)
                    ultimaFraseColocada = fraseEscolhida
                    candidatos.add(melhorEscolha)
                    somaLarguras += melhorEscolha.third
                } else {
                    break
                }
            }

            if (candidatos.isNotEmpty()) {
                if (candidatos.size == 1) {
                    val cand = candidatos[0]
                    val sobra = larguraDisponivel - cand.third
                    val xPos = xInicioT + (sobra / 2f)
                    val yPos = yTop + ((alturaLinha - cand.second) / 2f)

                    val centroFraseX = xPos + (cand.third / 2f)
                    val centroFraseY = yPos + (cand.second / 2f)
                    val dist = sqrt(((centroFraseX - centroX) * (centroFraseX - centroX)) + ((centroFraseY - centroY) * (centroFraseY - centroY)))
                    val distNorm = (dist / dMax).coerceIn(0f, 1f)

                    resultado.add(
                        FraseColocada(
                            texto = cand.first.texto,
                            x = xPos,
                            y = yPos,
                            tamanhoPx = cand.second,
                            distanciaDoCentro = distNorm,
                            ehCentral = false,
                        ),
                    )
                } else {
                    val sobra = larguraDisponivel - somaLarguras
                    val espaco = sobra / (candidatos.size - 1)
                    var xAtual = xInicioT

                    for (cand in candidatos) {
                        val yPos = yTop + ((alturaLinha - cand.second) / 2f)
                        val centroFraseX = xAtual + (cand.third / 2f)
                        val centroFraseY = yPos + ((cand.second) / 2f)
                        val dist = sqrt(((centroFraseX - centroX) * (centroFraseX - centroX)) + ((centroFraseY - centroY) * (centroFraseY - centroY)))
                        val distNorm = (dist / dMax).coerceIn(0f, 1f)

                        resultado.add(
                            FraseColocada(
                                texto = cand.first.texto,
                                x = xAtual,
                                y = yPos,
                                tamanhoPx = cand.second,
                                distanciaDoCentro = distNorm,
                                ehCentral = false,
                            ),
                        )
                        xAtual += cand.third + espaco
                    }
                }
            }
        }

        yLinha += alturaLinha
    }

    return resultado
}

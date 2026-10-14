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

    val minSpPx = 8f * densidade
    val t1 = max(0.024f * larguraCoracaoPx, minSpPx)
    val t2 = max(0.029f * larguraCoracaoPx, minSpPx)
    val t3 = max(0.034f * larguraCoracaoPx, minSpPx)
    val tamanhosPequenosPx = listOf(t1, t2, t3)

    // a) FRASE CENTRAL e ZONA RESERVADA
    val centroX = mapeador.centroTelaX
    val centroY = mapeador.centroTelaY

    // Calcula largura do coração na altura do centro
    var xEsquerdaCentral = centroX
    while ((xEsquerdaCentral > 0f) && mapeador.pontoTelaEstaDentro(xEsquerdaCentral, centroY)) {
        xEsquerdaCentral -= 1f
    }
    var xDireitaCentral = centroX
    while ((xDireitaCentral < larguraTela) && mapeador.pontoTelaEstaDentro(xDireitaCentral, centroY)) {
        xDireitaCentral += 1f
    }
    val larguraNaAlturaCentral = max(1f, xDireitaCentral - xEsquerdaCentral)

    var tamanhoCentralPx = 0.55f * larguraNaAlturaCentral
    var wCentral: Float
    var hCentral: Float

    while (tamanhoCentralPx > 2.5f * t3) {
        wCentral = medirLargura(FRASE_CENTRAL, tamanhoCentralPx)
        hCentral = tamanhoCentralPx
        val xCentralTmp = centroX - wCentral / 2f
        val yCentralTmp = centroY - hCentral / 2f

        if (wCentral <= 0.55f * larguraNaAlturaCentral &&
            mapeador.pontoTelaEstaDentro(xCentralTmp, yCentralTmp) &&
            mapeador.pontoTelaEstaDentro(xCentralTmp + wCentral, yCentralTmp) &&
            mapeador.pontoTelaEstaDentro(xCentralTmp, yCentralTmp + hCentral) &&
            mapeador.pontoTelaEstaDentro(xCentralTmp + wCentral, yCentralTmp + hCentral)
        ) {
            break
        }
        tamanhoCentralPx -= 1f
    }
    tamanhoCentralPx = max(tamanhoCentralPx, 2.5f * t3)
    wCentral = medirLargura(FRASE_CENTRAL, tamanhoCentralPx)
    hCentral = tamanhoCentralPx

    val xCentral = centroX - wCentral / 2f
    val yCentral = centroY - hCentral / 2f

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

    val margemZona = hCentral * 0.15f
    val zonaReservada = Retangulo(
        xMin = xCentral - margemZona,
        yMin = yCentral - margemZona,
        xMax = xCentral + wCentral + margemZona,
        yMax = yCentral + hCentral + margemZona,
    )

    val alturaLinha = t3 * 1.25f
    val yInicio = mapeador.matParaTelaY(mapeador.caixa.maxY)
    val yFim = mapeador.matParaTelaY(mapeador.caixa.minY)

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

    fun espiarProximaFrase(): Frase {
        if (baralhoAtual.isEmpty()) {
            baralhoAtual = criarBaralho()
        }
        return baralhoAtual.first()
    }

    fun consumirProximaFrase(): Frase {
        if (baralhoAtual.isEmpty()) {
            baralhoAtual = criarBaralho()
        }
        val f = baralhoAtual.removeAt(0)
        ultimaFraseColocada = f
        return f
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

            val candidatos = mutableListOf<Triple<Frase, Float, Float>>()
            var somaLarguras = 0f

            while (true) {
                val fraseCand = espiarProximaFrase()
                val tamPxCand = tamanhosPequenosPx[rng.nextInt(tamanhosPequenosPx.size)]
                val largCand = medirLargura(fraseCand.texto, tamPxCand)

                val espacoMinimo = 4f * densidade
                val espacoNecessario = if (candidatos.isEmpty()) largCand else somaLarguras + largCand + espacoMinimo

                if (espacoNecessario <= larguraDisponivel) {
                    consumirProximaFrase()
                    candidatos.add(Triple(fraseCand, tamPxCand, largCand))
                    somaLarguras += largCand
                } else {
                    break
                }
            }

            if (candidatos.isNotEmpty()) {
                if (candidatos.size == 1) {
                    val cand = candidatos[0]
                    val sobra = larguraDisponivel - cand.third
                    val xPos = xInicioT + sobra / 2f
                    val yPos = yTop + (alturaLinha - cand.second) / 2f

                    val centroFraseX = xPos + cand.third / 2f
                    val centroFraseY = yPos + cand.second / 2f
                    val dist = sqrt((centroFraseX - centroX) * (centroFraseX - centroX) + (centroFraseY - centroY) * (centroFraseY - centroY))
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
                        val yPos = yTop + (alturaLinha - cand.second) / 2f
                        val centroFraseX = xAtual + cand.third / 2f
                        val centroFraseY = yPos + cand.second / 2f
                        val dist = sqrt((centroFraseX - centroX) * (centroFraseX - centroX) + (centroFraseY - centroY) * (centroFraseY - centroY))
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

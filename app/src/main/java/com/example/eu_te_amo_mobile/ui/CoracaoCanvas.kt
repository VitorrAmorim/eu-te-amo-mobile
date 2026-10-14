package com.example.eu_te_amo_mobile.ui

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.eu_te_amo_mobile.dados.LISTA_FRASES
import com.example.eu_te_amo_mobile.dominio.FraseColocada
import com.example.eu_te_amo_mobile.dominio.calcularLayoutDeFrases
import kotlin.math.max
import kotlin.random.Random
import kotlin.system.measureTimeMillis

const val DURACAO_ANIMACAO_MS = 8000L

private data class ItemDesenhoAnimado(
    val textLayoutResult: TextLayoutResult,
    val xFinal: Float,
    val yFinal: Float,
    val corBase: Color,
    val ehCentral: Boolean,
    val instanteInicio: Float,
    val duracaoFracao: Float,
    val deslocamentoYPx: Float,
)

private data class CacheLayoutCoracao(
    val itens: List<ItemDesenhoAnimado>,
    val totalFrases: Int,
    val tempoCalculoMs: Long,
)

private fun lerpColor(c1: Color, c2: Color, t: Float): Color {
    val r = c1.red + ((c2.red - c1.red) * t)
    val g = c1.green + ((c2.green - c1.green) * t)
    val b = c1.blue + ((c2.blue - c1.blue) * t)
    return Color(r, g, b, 1f)
}

private fun calcularCorFrase(
    distanciaDoCentro: Float,
    ehCentral: Boolean,
    indice: Int,
    semente: Long = 42L,
): Color {
    if (ehCentral) return Color(0xFFFFF1F3)

    val corCentro = Color(0xFF8E1B3A)
    val corContorno = Color(0xFFFF4D6D)
    val corBase = lerpColor(corCentro, corContorno, distanciaDoCentro.coerceIn(0f, 1f))

    val rng = Random(semente + indice)
    val fatorBrilho = 0.90f + (rng.nextFloat() * 0.20f)

    return Color(
        red = (corBase.red * fatorBrilho).coerceIn(0f, 1f),
        green = (corBase.green * fatorBrilho).coerceIn(0f, 1f),
        blue = (corBase.blue * fatorBrilho).coerceIn(0f, 1f),
        alpha = 1f,
    )
}

@Composable
fun CoracaoCanvas(
    modifier: Modifier = Modifier,
    semente: Long = 42L,
    onLayoutCalculado: ((totalFrases: Int, tempoMs: Long) -> Unit)? = null,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val progressoAnimacao = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (progressoAnimacao.value < 1f) {
            progressoAnimacao.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = DURACAO_ANIMACAO_MS.toInt(),
                    easing = LinearEasing,
                ),
            )
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val larguraPx = constraints.maxWidth.toFloat()
        val alturaPx = constraints.maxHeight.toFloat()

        val cacheLayout = remember(larguraPx, alturaPx, density.density) {
            if ((larguraPx <= 0f) || (alturaPx <= 0f)) {
                CacheLayoutCoracao(emptyList(), 0, 0L)
            } else {
                var listaColocada: List<FraseColocada> = emptyList()

                val medirLargura: (String, Float) -> Float = { texto, tamanhoPx ->
                    val fontSizeSp = (tamanhoPx / density.density).sp
                    val layout = textMeasurer.measure(
                        text = texto,
                        style = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSizeSp,
                        ),
                    )
                    layout.size.width.toFloat()
                }

                val tempoMs = measureTimeMillis {
                    listaColocada = calcularLayoutDeFrases(
                        larguraTela = larguraPx,
                        alturaTela = alturaPx,
                        listaFrases = LISTA_FRASES,
                        medirLargura = medirLargura,
                        semente = semente,
                        densidade = density.density,
                    )
                }

                val totalFrases = listaColocada.size

                val frasesPequenas = listaColocada.filter { !it.ehCentral }
                val yMin = frasesPequenas.minOfOrNull { it.y } ?: 0f
                val yMax = frasesPequenas.maxOfOrNull { it.y } ?: 1f
                val intervaloY = max(1f, yMax - yMin)

                val alturaCoracaoAprox = max(1f, alturaPx * 0.88f)
                val deslocamentoFlutuacaoPx = alturaCoracaoAprox * 0.015f

                val itens = listaColocada.mapIndexed { index, fraseColocada ->
                    val fontSizeSp = (fraseColocada.tamanhoPx / density.density).sp
                    val cor = calcularCorFrase(
                        distanciaDoCentro = fraseColocada.distanciaDoCentro,
                        ehCentral = fraseColocada.ehCentral,
                        indice = index,
                        semente = semente,
                    )
                    val textLayoutResult = textMeasurer.measure(
                        text = fraseColocada.texto,
                        style = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSizeSp,
                        ),
                    )

                    val instanteInicio: Float
                    val duracaoFracao: Float
                    val deslocamentoY: Float

                    if (fraseColocada.ehCentral) {
                        instanteInicio = 0.70f
                        duracaoFracao = 0.30f
                        deslocamentoY = 0f
                    } else {
                        val progressoBaseAltura = (yMax - fraseColocada.y) / intervaloY
                        val baseStart = progressoBaseAltura * 0.50f

                        val rng = Random(semente + index)
                        val ruido = (rng.nextFloat() - 0.5f) * 0.15f
                        instanteInicio = (baseStart + ruido).coerceIn(0.0f, 0.50f)
                        duracaoFracao = 0.25f
                        deslocamentoY = deslocamentoFlutuacaoPx
                    }

                    ItemDesenhoAnimado(
                        textLayoutResult = textLayoutResult,
                        xFinal = fraseColocada.x,
                        yFinal = fraseColocada.y,
                        corBase = cor,
                        ehCentral = fraseColocada.ehCentral,
                        instanteInicio = instanteInicio,
                        duracaoFracao = duracaoFracao,
                        deslocamentoYPx = deslocamentoY,
                    )
                }

                Log.d("CoracaoCanvas", "Total frases colocadas: $totalFrases, tempo: ${tempoMs}ms")
                onLayoutCalculado?.invoke(totalFrases, tempoMs)

                CacheLayoutCoracao(itens, totalFrases, tempoMs)
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val progressoGlobal = progressoAnimacao.value

            drawRect(color = Color(0xFF14070C))

            for (item in cacheLayout.itens) {
                if (progressoGlobal < item.instanteInicio) continue

                val progressoLocal = ((progressoGlobal - item.instanteInicio) / item.duracaoFracao).coerceIn(0f, 1f)
                if (progressoLocal <= 0f) continue

                val umMenosT = 1f - progressoLocal
                val easeOut = 1f - (umMenosT * umMenosT * umMenosT)

                if (item.ehCentral) {
                    val escala = 0.9f + (0.1f * easeOut)
                    val corCentral = item.corBase.copy(alpha = easeOut)

                    val xCentroText = item.xFinal + (item.textLayoutResult.size.width / 2f)
                    val yCentroText = item.yFinal + (item.textLayoutResult.size.height / 2f)

                    withTransform(
                        transformBlock = {
                            scale(escala, escala, pivot = Offset(xCentroText, yCentroText))
                        },
                    ) {
                        drawText(
                            textLayoutResult = item.textLayoutResult,
                            color = corCentral,
                            topLeft = Offset(item.xFinal, item.yFinal),
                        )
                    }
                } else {
                    val yAtual = item.yFinal + (item.deslocamentoYPx * (1f - easeOut))
                    val cor = item.corBase.copy(alpha = easeOut * item.corBase.alpha)

                    drawText(
                        textLayoutResult = item.textLayoutResult,
                        color = cor,
                        topLeft = Offset(item.xFinal, yAtual),
                    )
                }
            }
        }
    }
}

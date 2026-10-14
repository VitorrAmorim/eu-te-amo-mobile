package com.example.eu_te_amo_mobile.ui

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import kotlin.random.Random
import kotlin.system.measureTimeMillis

private data class ItemDesenho(
    val textLayoutResult: TextLayoutResult,
    val x: Float,
    val y: Float,
    val cor: Color,
)

private data class CacheLayoutCoracao(
    val itens: List<ItemDesenho>,
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
                    ItemDesenho(
                        textLayoutResult = textLayoutResult,
                        x = fraseColocada.x,
                        y = fraseColocada.y,
                        cor = cor,
                    )
                }

                Log.d("CoracaoCanvas", "Total frases colocadas: $totalFrases, tempo: ${tempoMs}ms")
                onLayoutCalculado?.invoke(totalFrases, tempoMs)

                CacheLayoutCoracao(itens, totalFrases, tempoMs)
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color(0xFF14070C))
            for (item in cacheLayout.itens) {
                drawText(
                    textLayoutResult = item.textLayoutResult,
                    color = item.cor,
                    topLeft = Offset(item.x, item.y),
                )
            }
        }
    }
}

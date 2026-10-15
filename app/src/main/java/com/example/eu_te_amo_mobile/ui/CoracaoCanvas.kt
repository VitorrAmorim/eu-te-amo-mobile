package com.example.eu_te_amo_mobile.ui

import android.provider.Settings
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.eu_te_amo_mobile.R
import com.example.eu_te_amo_mobile.dados.LISTA_FRASES
import com.example.eu_te_amo_mobile.dominio.FraseColocada
import com.example.eu_te_amo_mobile.dominio.MapeadorCoracao
import com.example.eu_te_amo_mobile.dominio.calcularLayoutDeFrases
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random
import kotlin.system.measureTimeMillis

const val DURACAO_ANIMACAO_MS = 8000L
const val PARTICULAS_ATIVAS = true

private data class ParticulaLuz(
    val relX: Float,
    val relYInicial: Float,
    val raioDp: Float,
    val velocidade: Float,
    val opacidadeMax: Float,
)

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
    val particulas: List<ParticulaLuz>,
    val totalFrases: Int,
    val frasesPequenasCount: Int,
    val centroX: Float,
    val centroY: Float,
    val larguraCoracaoPx: Float,
    val tempoCalculoMs: Long,
)

private fun calcularCorEProfundidadeFrase(
    distanciaDoCentro: Float,
    ehCentral: Boolean,
    tamanhoPx: Float,
    tMinPx: Float,
    tMaxPx: Float,
    indice: Int,
    semente: Long = 42L,
): Color {
    if (ehCentral) return Color(0xFFFFF1F3)

    val rng = Random(semente + indice)
    val eChampagne = rng.nextFloat() < 0.08f

    val corTom = if (eChampagne) {
        Color(0xFFF3D9B1)
    } else {
        val tonsRose = listOf(Color(0xFFFF6B81), Color(0xFFFF8FA3), Color(0xFFFFB3C1))
        tonsRose[rng.nextInt(tonsRose.size)]
    }

    val fTamanho = if (tMaxPx > tMinPx) ((tamanhoPx - tMinPx) / (tMaxPx - tMinPx)).coerceIn(0f, 1f) else 0.5f
    var opacidadeBase = 0.55f + (0.40f * fTamanho)

    if (distanciaDoCentro > 0.85f) {
        opacidadeBase += 0.10f
    }

    val opacidadeFinal = opacidadeBase.coerceIn(0f, 1f)
    return corTom.copy(alpha = opacidadeFinal)
}

@Composable
fun CoracaoCanvas(
    modifier: Modifier = Modifier,
    semente: Long = 42L,
    onLayoutCalculado: ((totalFrases: Int, tempoMs: Long) -> Unit)? = null,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val descricaoAcessibilidade = stringResource(R.string.descricao_coracao_canvas)

    val animacoesAtivas = remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) > 0f
        } catch (_: Exception) {
            true
        }
    }

    val progressoAnimacao = remember { Animatable(if (animacoesAtivas) 0f else 1f) }
    val tempoPosAnimacaoMs = remember { mutableLongStateOf(0L) }

    val reiniciarAnimacao: () -> Unit = {
        if (animacoesAtivas && (progressoAnimacao.value >= 1f)) {
            scope.launch {
                progressoAnimacao.snapTo(0f)
                progressoAnimacao.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = DURACAO_ANIMACAO_MS.toInt(),
                        easing = LinearEasing,
                    ),
                )
            }
        }
    }

    LaunchedEffect(animacoesAtivas) {
        if (animacoesAtivas) {
            if (progressoAnimacao.value < 1f) {
                progressoAnimacao.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = DURACAO_ANIMACAO_MS.toInt(),
                        easing = LinearEasing,
                    ),
                )
            }
        } else {
            progressoAnimacao.snapTo(1f)
        }
    }

    LaunchedEffect(animacoesAtivas, lifecycleOwner) {
        if (animacoesAtivas) {
            snapshotFlow { progressoAnimacao.value >= 1f }
                .collect { entradaConcluida ->
                    if (entradaConcluida) {
                        var tempoInicialNanos = 0L
                        while (isActive) {
                            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                                withFrameNanos { frameTimeNanos ->
                                    if (tempoInicialNanos == 0L) {
                                        tempoInicialNanos = frameTimeNanos
                                    }
                                    tempoPosAnimacaoMs.longValue = (frameTimeNanos - tempoInicialNanos) / 1_000_000L
                                }
                            }
                        }
                    }
                }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val larguraPx = constraints.maxWidth.toFloat()
        val alturaPx = constraints.maxHeight.toFloat()

        val cacheLayout = remember(larguraPx, alturaPx, density.density) {
            if ((larguraPx <= 0f) || (alturaPx <= 0f)) {
                CacheLayoutCoracao(emptyList(), emptyList(), 0, 0, 0f, 0f, 0f, 0L)
            } else {
                var listaColocada: List<FraseColocada> = emptyList()

                val medirLarguraComum: (String, Float) -> Float = { texto, tamanhoPx ->
                    val fontSizeSp = (tamanhoPx / density.density).sp
                    val layout = textMeasurer.measure(
                        text = texto,
                        style = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Medium,
                            fontSize = fontSizeSp,
                        ),
                    )
                    layout.size.width.toFloat()
                }

                val medirLarguraCentral: (String, Float) -> Float = { texto, tamanhoPx ->
                    val fontSizeSp = (tamanhoPx / density.density).sp
                    val layout = textMeasurer.measure(
                        text = texto,
                        style = TextStyle(
                            fontFamily = FonteCentralFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = fontSizeSp,
                        ),
                    )
                    layout.size.width.toFloat()
                }

                val medirAlturaCentral: (String, Float) -> Float = { texto, tamanhoPx ->
                    val fontSizeSp = (tamanhoPx / density.density).sp
                    val layout = textMeasurer.measure(
                        text = texto,
                        style = TextStyle(
                            fontFamily = FonteCentralFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = fontSizeSp,
                        ),
                    )
                    layout.size.height.toFloat()
                }

                val tempoMs = measureTimeMillis {
                    listaColocada = calcularLayoutDeFrases(
                        larguraTela = larguraPx,
                        alturaTela = alturaPx,
                        listaFrases = LISTA_FRASES,
                        medirLarguraComum = medirLarguraComum,
                        medirLarguraCentral = medirLarguraCentral,
                        medirAlturaCentral = medirAlturaCentral,
                        semente = semente,
                        densidade = density.density,
                    )
                }

                val mapeador = MapeadorCoracao(larguraPx, alturaPx)
                val centroX = mapeador.centroTelaX
                val centroY = mapeador.centroTelaY
                val larguraCoracaoPx = mapeador.larguraCoracaoPx

                val totalFrases = listaColocada.size
                val frasesPequenas = listaColocada.filter { !it.ehCentral }
                val frasesPequenasCount = frasesPequenas.size

                val minSpPx = 7f * density.density
                val t1 = max(0.024f * larguraCoracaoPx, minSpPx)
                val t2 = max(0.029f * larguraCoracaoPx, minSpPx)
                val t3 = max(0.034f * larguraCoracaoPx, minSpPx)
                val tMinPx = minOf(t1, t2, t3)
                val tMaxPx = maxOf(t1, t2, t3)

                val yMin = frasesPequenas.minOfOrNull { it.y } ?: 0f
                val yMax = frasesPequenas.maxOfOrNull { it.y } ?: 1f
                val intervaloY = max(1f, yMax - yMin)

                val alturaCoracaoAprox = max(1f, alturaPx * 0.88f)
                val deslocamentoFlutuacaoPx = alturaCoracaoAprox * 0.015f

                val itens = listaColocada.mapIndexed { index, fraseColocada ->
                    val fontSizeSp = (fraseColocada.tamanhoPx / density.density).sp
                    val cor = calcularCorEProfundidadeFrase(
                        distanciaDoCentro = fraseColocada.distanciaDoCentro,
                        ehCentral = fraseColocada.ehCentral,
                        tamanhoPx = fraseColocada.tamanhoPx,
                        tMinPx = tMinPx,
                        tMaxPx = tMaxPx,
                        indice = index,
                        semente = semente,
                    )

                    val textStyle = if (fraseColocada.ehCentral) {
                        val shadowGlow = Shadow(
                            color = Color(0xFFFF4D6D),
                            offset = Offset.Zero,
                            blurRadius = fontSizeSp.value * density.density * 0.35f,
                        )
                        TextStyle(
                            fontFamily = FonteCentralFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = fontSizeSp,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFFFF6F0), Color(0xFFFFC2D1)),
                            ),
                            shadow = shadowGlow,
                        )
                    } else {
                        TextStyle(
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Medium,
                            fontSize = fontSizeSp,
                        )
                    }

                    val textLayoutResult = textMeasurer.measure(
                        text = fraseColocada.texto,
                        style = textStyle,
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

                val rngPart = Random(42L)
                val particulas = List(18) {
                    ParticulaLuz(
                        relX = rngPart.nextFloat(),
                        relYInicial = rngPart.nextFloat(),
                        raioDp = 6f + (rngPart.nextFloat() * 16f),
                        velocidade = 1f / (25000f + (rngPart.nextFloat() * 15000f)),
                        opacidadeMax = 0.04f + (rngPart.nextFloat() * 0.08f),
                    )
                }

                Log.d("CoracaoCanvas", "Total frases colocadas: $totalFrases, tempo: ${tempoMs}ms")
                onLayoutCalculado?.invoke(totalFrases, tempoMs)

                CacheLayoutCoracao(
                    itens = itens,
                    particulas = particulas,
                    totalFrases = totalFrases,
                    frasesPequenasCount = frasesPequenasCount,
                    centroX = centroX,
                    centroY = centroY,
                    larguraCoracaoPx = larguraCoracaoPx,
                    tempoCalculoMs = tempoMs,
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = descricaoAcessibilidade
                }
                .pointerInput(animacoesAtivas) {
                    detectTapGestures {
                        reiniciarAnimacao()
                    }
                },
        ) {
            val progressoGlobal = progressoAnimacao.value
            val tPosAnimMs = tempoPosAnimacaoMs.longValue
            val posAnimacaoAtiva = animacoesAtivas && (progressoGlobal >= 1f)

            val centroX = cacheLayout.centroX
            val centroY = cacheLayout.centroY

            val raioFundo = max(larguraPx, alturaPx) * 0.85f
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF3A1030), Color(0xFF12060F)),
                    center = Offset(centroX, centroY),
                    radius = raioFundo,
                ),
            )

            val raioGlowCoracao = max(1f, cacheLayout.larguraCoracaoPx * 0.60f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF4D6D).copy(alpha = 0.16f),
                        Color(0xFFFF4D6D).copy(alpha = 0f),
                    ),
                    center = Offset(centroX, centroY),
                    radius = raioGlowCoracao,
                ),
                center = Offset(centroX, centroY),
                radius = raioGlowCoracao,
            )

            if (PARTICULAS_ATIVAS && animacoesAtivas) {
                for (p in cacheLayout.particulas) {
                    val deslocamentoYRel = (tPosAnimMs.toFloat() * p.velocidade) % 1f
                    var relY = p.relYInicial - deslocamentoYRel
                    if (relY < 0f) relY += 1f

                    val xPx = p.relX * larguraPx
                    val yPx = relY * alturaPx
                    val raioPx = p.raioDp * density.density

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF4D6D).copy(alpha = p.opacidadeMax),
                                Color(0xFFFF4D6D).copy(alpha = 0f),
                            ),
                            center = Offset(xPx, yPx),
                            radius = raioPx,
                        ),
                        center = Offset(xPx, yPx),
                        radius = raioPx,
                    )
                }
            }

            val opacidadeHalo: Float
            val margemHaloPx: Float
            if (posAnimacaoAtiva) {
                val faseHalo = (tPosAnimMs % 2500L).toFloat() / 2500f
                val ondaHalo = (sin(faseHalo * 2f * PI.toFloat()) + 1f) / 2f
                opacidadeHalo = 0.05f + (0.15f * ondaHalo)
                margemHaloPx = 16f * density.density
            } else {
                opacidadeHalo = 0f
                margemHaloPx = 0f
            }

            val totalFrasesPequenas = cacheLayout.frasesPequenasCount
            val indiceFraseBrilho: Int
            val intensidadeBrilho: Float
            if (posAnimacaoAtiva && (totalFrasesPequenas > 0)) {
                val cicloBrilho = tPosAnimMs / 400L
                val tCiclo = (tPosAnimMs % 400L).toFloat() / 400f
                intensidadeBrilho = sin(tCiclo * PI.toFloat())
                val rngCiclo = Random(semente + cicloBrilho)
                indiceFraseBrilho = rngCiclo.nextInt(totalFrasesPequenas)
            } else {
                indiceFraseBrilho = -1
                intensidadeBrilho = 0f
            }

            var indicePequenaAtual = 0
            for (item in cacheLayout.itens) {
                if (progressoGlobal < item.instanteInicio) continue

                val progressoLocal = if (progressoGlobal >= 1f) 1f else ((progressoGlobal - item.instanteInicio) / item.duracaoFracao).coerceIn(0f, 1f)
                if (progressoLocal <= 0f) continue

                val umMenosT = 1f - progressoLocal
                val easeOut = 1f - (umMenosT * umMenosT * umMenosT)

                if (item.ehCentral) {
                    val escala = 0.9f + (0.1f * easeOut)
                    val corCentralAlpha = Color.White.copy(alpha = easeOut)

                    val xCentroText = item.xFinal + (item.textLayoutResult.size.width / 2f)
                    val yCentroText = item.yFinal + (item.textLayoutResult.size.height / 2f)

                    if (posAnimacaoAtiva && (opacidadeHalo > 0f)) {
                        val xHalo = item.xFinal - margemHaloPx
                        val yHalo = item.yFinal - margemHaloPx
                        val wHalo = item.textLayoutResult.size.width + (2f * margemHaloPx)
                        val hHalo = item.textLayoutResult.size.height + (2f * margemHaloPx)

                        drawRoundRect(
                            color = Color(0xFFFF4D6D).copy(alpha = opacidadeHalo),
                            topLeft = Offset(xHalo, yHalo),
                            size = Size(wHalo, hHalo),
                            cornerRadius = CornerRadius(20f * density.density),
                        )
                    }

                    withTransform(
                        transformBlock = {
                            scale(escala, escala, pivot = Offset(xCentroText, yCentroText))
                        },
                    ) {
                        drawText(
                            textLayoutResult = item.textLayoutResult,
                            color = corCentralAlpha,
                            topLeft = Offset(item.xFinal, item.yFinal),
                        )
                    }
                } else {
                    val yAtual = item.yFinal + (item.deslocamentoYPx * (1f - easeOut))
                    var cor = item.corBase.copy(alpha = easeOut * item.corBase.alpha)

                    if (posAnimacaoAtiva && (indicePequenaAtual == indiceFraseBrilho) && (intensidadeBrilho > 0f)) {
                        val fatorClarear = 1f + (0.35f * intensidadeBrilho)
                        cor = Color(
                            red = (cor.red * fatorClarear).coerceIn(0f, 1f),
                            green = (cor.green * fatorClarear).coerceIn(0f, 1f),
                            blue = (cor.blue * fatorClarear).coerceIn(0f, 1f),
                            alpha = cor.alpha,
                        )
                    }

                    drawText(
                        textLayoutResult = item.textLayoutResult,
                        color = cor,
                        topLeft = Offset(item.xFinal, yAtual),
                    )

                    indicePequenaAtual++
                }
            }
        }
    }
}

package edu.itsco.calculadora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.itsco.calculadora.ui.theme.CalculadoraTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalculadoraTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(title = { Text("Calculadora") })
                    }
                ) { innerPadding ->
                    CalculadoraApp(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

fun evaluarExpresion(expr: String): Double {
    val tokens = tokenizar(expr.replace(" ", ""))
    val (resultado, _) = parsearSuma(tokens, 0)
    return resultado
}

fun tokenizar(expr: String): List<String> {
    val lista = mutableListOf<String>()
    var i = 0
    while (i < expr.length) {
        when {
            expr[i].isDigit() || expr[i] == '.' -> {
                var num = ""
                while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                    num += expr[i]; i++
                }
                lista.add(num)
            }
            expr[i] == '-' && (lista.isEmpty() || lista.last() in listOf("+", "-", "*", "/", "(")) -> {
                var num = "-"; i++
                while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                    num += expr[i]; i++
                }
                lista.add(num)
            }
            else -> { lista.add(expr[i].toString()); i++ }
        }
    }
    return lista
}

fun parsearSuma(tokens: List<String>, pos: Int): Pair<Double, Int> {
    var (izq, i) = parsearMulti(tokens, pos)
    while (i < tokens.size && (tokens[i] == "+" || tokens[i] == "-")) {
        val op = tokens[i]
        val (der, j) = parsearMulti(tokens, i + 1)
        izq = if (op == "+") izq + der else izq - der
        i = j
    }
    return Pair(izq, i)
}

fun parsearMulti(tokens: List<String>, pos: Int): Pair<Double, Int> {
    var (izq, i) = parsearAtomo(tokens, pos)
    while (i < tokens.size && (tokens[i] == "*" || tokens[i] == "/")) {
        val op = tokens[i]
        val (der, j) = parsearAtomo(tokens, i + 1)
        izq = if (op == "*") izq * der else {
            if (der == 0.0) throw ArithmeticException("División entre cero")
            izq / der
        }
        i = j
    }
    return Pair(izq, i)
}

fun parsearAtomo(tokens: List<String>, pos: Int): Pair<Double, Int> {
    if (pos >= tokens.size) return Pair(0.0, pos)
    return if (tokens[pos] == "(") {
        val (valor, i) = parsearSuma(tokens, pos + 1)
        val siguiente = if (i < tokens.size && tokens[i] == ")") i + 1 else i
        Pair(valor, siguiente)
    } else {
        Pair(tokens[pos].toDoubleOrNull() ?: 0.0, pos + 1)
    }
}

@Composable
fun CalculadoraApp(modifier: Modifier = Modifier) {

    var display            by remember { mutableStateOf("0") }
    var expresionVisible   by remember { mutableStateOf("") }
    var expresionInterna   by remember { mutableStateOf("") }
    var tienePunto         by remember { mutableStateOf(false) }
    var clicksC            by remember { mutableIntStateOf(0) }
    var parentesisAbiertos by remember { mutableIntStateOf(0) }

    fun presionarNumero(num: String) {
        clicksC = 0
        display = if (display == "0" || display == "Error") num else display + num
    }

    fun presionarPunto() {
        clicksC = 0
        if (!tienePunto) { display += "."; tienePunto = true }
    }

    fun cambiarSigno() {
        clicksC = 0
        val n = display.toDoubleOrNull() ?: return
        display = if (n % 1.0 == 0.0) (n * -1).toInt().toString()
        else (n * -1).toString()
    }

    fun presionarPorcentaje() {
        clicksC = 0
        val n = display.toDoubleOrNull() ?: return
        val r = n / 100.0
        display = if (r % 1.0 == 0.0) r.toInt().toString() else r.toString()
    }

    fun presionarParentesisAbre() {
        clicksC = 0
        expresionVisible += "("
        expresionInterna += "("
        parentesisAbiertos++
        display = "0"
        tienePunto = false
    }

    fun presionarParentesisCierra() {
        clicksC = 0
        if (parentesisAbiertos > 0) {
            expresionVisible += display + ")"
            expresionInterna += display + ")"
            parentesisAbiertos--
            display = "0"
            tienePunto = false
        }
    }

    fun presionarOperador(simboloVisible: String, simboloInterno: String) {
        clicksC    = 0
        tienePunto = false
        if (expresionVisible.endsWith(")")) {
            expresionVisible += simboloVisible
            expresionInterna += simboloInterno
        } else {
            expresionVisible += display + simboloVisible
            expresionInterna += display + simboloInterno
        }
        display = "0"
    }

    fun calcularResultado() {
        clicksC = 0
        val exprFinal = if (expresionInterna.endsWith(")")) expresionInterna
        else expresionInterna + display

        expresionVisible = if (expresionVisible.endsWith(")")) expresionVisible + "="
        else expresionVisible + display + "="

        val resultado = try {
            evaluarExpresion(exprFinal)
        } catch (e: Exception) {
            display = "Error"; expresionVisible = ""; expresionInterna = ""
            parentesisAbiertos = 0; return
        }

        display = if (resultado % 1.0 == 0.0) resultado.toInt().toString()
        else resultado.toString()

        tienePunto         = display.contains(".")
        expresionInterna   = ""
        parentesisAbiertos = 0
    }

    fun presionarC() {
        clicksC++
        if (clicksC >= 2) {
            display = "0"; expresionVisible = ""; expresionInterna = ""
            tienePunto = false; clicksC = 0; parentesisAbiertos = 0
        } else {
            if (display.length > 1) {
                if (display.last() == '.') tienePunto = false
                display = display.dropLast(1)
            } else { display = "0"; tienePunto = false }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text      = expresionVisible,
                fontSize  = 18.sp,
                textAlign = TextAlign.End,
                color     = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text      = display,
                fontSize  = 48.sp,
                textAlign = TextAlign.End
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BotonCalc("C")  { presionarC() }
            BotonCalc("(")  { presionarParentesisAbre() }
            BotonCalc(")")  { presionarParentesisCierra() }
            BotonCalc("%")  { presionarPorcentaje() }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BotonCalc("7") { presionarNumero("7") }
            BotonCalc("8") { presionarNumero("8") }
            BotonCalc("9") { presionarNumero("9") }
            BotonCalc("÷") { presionarOperador("÷", "/") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BotonCalc("4") { presionarNumero("4") }
            BotonCalc("5") { presionarNumero("5") }
            BotonCalc("6") { presionarNumero("6") }
            BotonCalc("×") { presionarOperador("×", "*") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BotonCalc("1") { presionarNumero("1") }
            BotonCalc("2") { presionarNumero("2") }
            BotonCalc("3") { presionarNumero("3") }
            BotonCalc("-") { presionarOperador("-", "-") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BotonCalc("+/-") { cambiarSigno() }
            BotonCalc("0")   { presionarNumero("0") }
            BotonCalc(".")   { presionarPunto() }
            BotonCalc("+")   { presionarOperador("+", "+") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ElevatedButton(
                onClick        = { calcularResultado() },
                modifier       = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(text = "=", fontSize = 24.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun RowScope.BotonCalc(texto: String, onClick: () -> Unit) {
    ElevatedButton(
        onClick        = onClick,
        modifier       = Modifier
            .weight(1f)
            .aspectRatio(1f),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text = texto, fontSize = 20.sp)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CalculadoraPreview() {
    CalculadoraTheme {
        CalculadoraApp()
    }
}
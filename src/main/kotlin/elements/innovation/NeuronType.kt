package elements.innovation

enum class NeuronType(val id : Int) {
    Input(0),
    Bias(1),
    Hidden(2),
    Output(3),
    Unknown(4)
}
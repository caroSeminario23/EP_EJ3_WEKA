import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.trees.J48;
import weka.classifiers.Evaluation;
import java.util.Random;
import weka.gui.visualize.VisualizePanel;
import weka.gui.visualize.PlotData2D;
import javax.swing.JFrame;

public class Entrenamiento {
    public static void main(String[] args) throws Exception {
        // Cargar el conjunto de datos
        String arffPath = "Data/DP_notas_categorizadas.arff";
        System.out.println("Leyendo ARFF en: " + arffPath);
        DataSource source = new DataSource(arffPath);

        // Instanciar el conjunto de datos
        Instances data = source.getDataSet();

        // Mostrar el conjunto de datos
        System.out.println(data);

        // Establecer el atributo a predecir (último atributo)
        if (data.classIndex() == -1) {
            data.setClassIndex(data.numAttributes() - 1);
        }

        // Entrenar el árbol de decisión J48 con estos datos
        entrenamientoJ48(data);

        // Visualizar gráfico de matriz de dispersión entre atributos
        VisualizePanel vp = new VisualizePanel(); // Crear un panel de visualización
        PlotData2D pd = new PlotData2D(data); // Crear un objeto de datos para el gráfico
        pd.setPlotName("Gráfico de dispersión de atributos"); // Establecer el nombre del gráfico
        vp.addPlot(pd); // Agregar los datos al panel de visualización

        JFrame frame = new JFrame("Visualización de Atributos"); // Crear un marco para mostrar el gráfico
        frame.setSize(800, 600); // Establecer el tamaño del marco
        frame.setContentPane(vp); // Establecer el panel de visualización como contenido del marco
        frame.setVisible(true); // Hacer visible el marco
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Cerrar el programa al cerrar el marco

        System.out.println("----------------------------------------");
        System.out.println("\nVisualización de atributos completada.");
    }

    // Entrenar un árbol de decisión J48
    public static void entrenamientoJ48(Instances dataset) throws Exception {
        // Instanciar el clasificador
        J48 tree = new J48();

        // Brindar el conjunto de datos
        tree.buildClassifier(dataset);

        // Evaluar el clasificador
        Evaluation eval = new Evaluation(dataset);
        eval.crossValidateModel(tree, dataset, 10, new Random(1)); // 10 partes
        System.out.println("----------------------------");
        System.out.println("Resultados de la evaluación:");
        System.out.println(eval.toSummaryString());
        System.out.println("----------------------------");
        System.out.println("Detalles de la precisión por clase:");
        System.out.println(eval.toClassDetailsString());
        System.out.println("----------------------------");
        System.out.println("Matriz de confusión:");
        System.out.println(eval.toMatrixString());

        // Mostrar el árbol de decisión
        System.out.println("----------------------------");
        System.out.println("Árbol de decisión J48:");
        System.out.println(tree);
        System.out.println("----------------------------");

        // Guardar el modelo entrenado
        weka.core.SerializationHelper.write("Model/notas_j48.model", tree);
        System.out.println("Modelo guardado como modelo_j48.model");
    }
}
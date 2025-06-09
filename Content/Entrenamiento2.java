import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.unsupervised.attribute.NumericToNominal;
import weka.classifiers.trees.J48;
import weka.classifiers.Evaluation;
import weka.gui.visualize.VisualizePanel;
import weka.gui.visualize.PlotData2D;
import weka.core.DenseInstance;
import weka.filters.Filter;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class Entrenamiento2 {
    public static void main(String[] args) throws Exception {
        String arffPath = "Data/DP_notas_categorizadas.arff";
        System.out.println("Leyendo ARFF en: " + arffPath);
        DataSource source = new DataSource(arffPath);
        Instances data = source.getDataSet();

        if (data.classIndex() == -1) {
            data.setClassIndex(data.numAttributes() - 1);
        }

        // --- Conversión de cod_escuela, cod_plan y cod_asignatura a nominal ---
        NumericToNominal n2n = new NumericToNominal();
        n2n.setAttributeIndices("1-3");   // ajusta estos índices según tu ARFF
        n2n.setInputFormat(data);
        data = Filter.useFilter(data, n2n);

        // Entrenar modelo
        J48 modelo = entrenamientoJ48(data);

        // Crear interfaz con pestañas
        crearInterfaz(data, modelo);
    }

    public static J48 entrenamientoJ48(Instances dataset) throws Exception {
        J48 tree = new J48();
        tree.buildClassifier(dataset);

        Evaluation eval = new Evaluation(dataset);
        eval.crossValidateModel(tree, dataset, 10, new Random(1));

        String resultados = "Resultados de la evaluación:\n" + eval.toSummaryString()
                + "\n\nDetalles por clase:\n" + eval.toClassDetailsString()
                + "\n\nMatriz de Confusión:\n" + eval.toMatrixString()
                + "\n\nÁrbol de Decisión:\n" + tree.toString();

        // Guardar el modelo
        weka.core.SerializationHelper.write("Model/notas_j48.model", tree);
        System.setProperty("weka.model.resultados", resultados);
        return tree;
    }

    public static void crearInterfaz(Instances data, J48 modelo) throws Exception {
        JFrame frame = new JFrame("Análisis de Rendimiento Académico - J48");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Datos", crearPanelDatos(data));
        tabs.addTab("Resultados", crearPanelResultados());
        tabs.addTab("Gráfico", crearPanelGrafico(data));
        tabs.addTab("Predicción", crearPanelPrediccion(modelo, data));

        frame.add(tabs);
        frame.setVisible(true);
    }

    public static JPanel crearPanelDatos(Instances data) {
        String[] columnas = new String[data.numAttributes()];
        for (int i = 0; i < columnas.length; i++) {
            columnas[i] = data.attribute(i).name();
        }

        String[][] filas = new String[data.numInstances()][data.numAttributes()];
        for (int i = 0; i < data.numInstances(); i++) {
            for (int j = 0; j < data.numAttributes(); j++) {
                filas[i][j] = data.instance(i).toString(j);
            }
        }

        JTable tabla = new JTable(filas, columnas);
        JScrollPane scroll = new JScrollPane(tabla);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    public static JPanel crearPanelResultados() {
        JTextArea area = new JTextArea(System.getProperty("weka.model.resultados"));
        area.setEditable(false);
        JScrollPane scroll = new JScrollPane(area);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    public static JPanel crearPanelGrafico(Instances data) throws Exception {
        VisualizePanel vp = new VisualizePanel();
        PlotData2D pd = new PlotData2D(data);
        pd.setPlotName("Gráfico de Dispersión");
        vp.addPlot(pd);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(vp, BorderLayout.CENTER);
        return panel;
    }

    public static JPanel crearPanelPrediccion(J48 modelo, Instances data) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Map<String, JComboBox<String>> campos = new LinkedHashMap<>();

        Set<String> escuelas = new HashSet<>();
        Map<String, Set<String>> escuelaToPlanes = new HashMap<>();
        Map<String, Set<String>> escuelaPlanToAsignaturas = new HashMap<>();

        for (int i = 0; i < data.numInstances(); i++) {
            String esc = data.instance(i).stringValue(data.attribute("cod_escuela"));
            String plan = data.instance(i).stringValue(data.attribute("cod_plan"));
            String asig = data.instance(i).stringValue(data.attribute("cod_asignatura"));

            escuelas.add(esc);
            escuelaToPlanes.computeIfAbsent(esc, k -> new HashSet<>()).add(plan);
            escuelaPlanToAsignaturas.computeIfAbsent(esc + ":" + plan, k -> new HashSet<>()).add(asig);
        }

        JComboBox<String> cbEscuela = new JComboBox<>(escuelas.toArray(new String[0]));
        JComboBox<String> cbPlan = new JComboBox<>();
        JComboBox<String> cbAsignatura = new JComboBox<>();

        cbEscuela.addActionListener(e -> {
            String escuela = (String) cbEscuela.getSelectedItem();
            cbPlan.removeAllItems();
            if (escuela != null) {
                for (String p : escuelaToPlanes.get(escuela)) cbPlan.addItem(p);
            }
        });

        cbPlan.addActionListener(e -> {
            String escuela = (String) cbEscuela.getSelectedItem();
            String plan = (String) cbPlan.getSelectedItem();
            cbAsignatura.removeAllItems();
            if (escuela != null && plan != null) {
                for (String a : escuelaPlanToAsignaturas.getOrDefault(escuela + ":" + plan, new HashSet<>())) {
                    cbAsignatura.addItem(a);
                }
            }
        });

        campos.put("Código de escuela", cbEscuela);
        campos.put("Plan", cbPlan);
        campos.put("Código de asignatura", cbAsignatura);

        for (Map.Entry<String, JComboBox<String>> entry : campos.entrySet()) {
            JLabel label = new JLabel(entry.getKey());
            JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
            p.add(label);
            p.add(entry.getValue());
            gbc.gridx = 0;
            gbc.gridy++;
            panel.add(p, gbc);
        }

        JButton btn = new JButton("Predecir");
        JLabel lblResultado = new JLabel("Tipo de nota probable: ");

        gbc.gridy++;
        panel.add(btn, gbc);
        gbc.gridy++;
        panel.add(lblResultado, gbc);

        btn.addActionListener(e -> {
            try {
                Instances estructura = data.stringFreeStructure();
                double[] valores = new double[estructura.numAttributes()];

                valores[estructura.attribute("cod_escuela").index()] = estructura.attribute("cod_escuela").indexOfValue((String) cbEscuela.getSelectedItem());
                valores[estructura.attribute("cod_plan").index()] = estructura.attribute("cod_plan").indexOfValue((String) cbPlan.getSelectedItem());
                valores[estructura.attribute("cod_asignatura").index()] = estructura.attribute("cod_asignatura").indexOfValue((String) cbAsignatura.getSelectedItem());
                valores[valores.length - 1] = Double.NaN;

                DenseInstance instancia = new DenseInstance(1.0, valores);
                instancia.setDataset(estructura);
                double pred = modelo.classifyInstance(instancia);
                String clase = estructura.classAttribute().value((int) pred);
                lblResultado.setText("Tipo de nota probable: " + clase);

            } catch (Exception ex) {
                lblResultado.setText("Error: " + ex.getMessage());
            }
        });

        return panel;
    }
}

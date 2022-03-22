package com.unipi.client.UI.pages;

import com.unipi.common.WinsomeTransaction;

import javax.swing.*;
import java.awt.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;

public class HistoryWallet extends JFrame {

    public HistoryWallet(LinkedList<WinsomeTransaction> list) {
        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> transactions = new JList<>(model);

        for (WinsomeTransaction transaction : list) {
            model.addElement(String.format("%s > %s", transaction.getCoins(), transaction.getDate()));
        }

        setTitle("Cronologia Incrementi");
        transactions.setFont(new Font("Arial", Font.PLAIN, 20));
        transactions.setDragEnabled(false);
        transactions.setCellRenderer(renderRowLine());
        transactions.setFixedCellHeight(40);

        JScrollPane scrollPane = new JScrollPane(transactions);
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);

        setMinimumSize(new Dimension(500, 720));
        setLocationRelativeTo(null);
        add(scrollPane);
    }

    private boolean checkList(JList<String> list) {
        for (int i = 0; i < list.getModel().getSize(); i++) {
            String s = list.getModel().getElementAt(i);

            if (!s.contains(" > "))
                return false;

            String date = s.split(" > ", 2)[1];

            try {
                SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy - hh:mm:ss");
                format.parse(date);
            } catch (ParseException e) {
                return false;
            }

        }

        return true;
    }

    public void open() {
        setVisible(true);
    }

    private ListCellRenderer<? super String> renderRowLine() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list,
                                                          Object value, int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                JLabel listCellRendererComponent = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                listCellRendererComponent.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));
                return listCellRendererComponent;
            }
        };
    }
}

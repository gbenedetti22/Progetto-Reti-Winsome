package com.unipi.client.UI.pages;

import javax.swing.*;
import java.awt.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class HistoryWallet extends JFrame {

    public HistoryWallet(JList<String> operations) throws IllegalArgumentException{
        if(!checkList(operations)){
            throw new IllegalArgumentException("Lista di operazioni passata non valida");
        }

        setTitle("Cronologia Incrementi");
        operations.setFont(new Font("Arial", Font.PLAIN, 20));
        operations.setDragEnabled(false);
        operations.setCellRenderer(renderRowLine());
        operations.setFixedCellHeight(40);

        JScrollPane scrollPane = new JScrollPane(operations);
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);

        setMinimumSize(new Dimension(500, 720));
        setLocationRelativeTo(null);
        add(scrollPane);
        setVisible(true);
    }

    public HistoryWallet(){
        this(new JList<>());
    }
    
    private boolean checkList(JList<String> list){
        for (int i = 0; i < list.getModel().getSize(); i++) {
            String s = list.getModel().getElementAt(i);

            if(!s.contains(" > "))
                return false;

            String date = s.split(" > ", 2)[1];

            try{
                SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy - hh:mm:ss");
                format.parse(date);
            }catch (ParseException e){
                return false;
            }

        }

        return true;
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

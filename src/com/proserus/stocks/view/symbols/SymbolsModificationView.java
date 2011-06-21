package com.proserus.stocks.view.symbols;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.jfree.data.time.Year;

import com.proserus.stocks.bp.SymbolsBp;
import com.proserus.stocks.controllers.PortfolioControllerImpl;
import com.proserus.stocks.model.symbols.CurrencyEnum;
import com.proserus.stocks.model.symbols.HistoricalPriceImpl;
import com.proserus.stocks.model.symbols.Symbol;
import com.proserus.stocks.view.common.AbstractDialog;
import com.proserus.stocks.view.common.verifiers.NumberVerifier;
import com.proserus.stocks.view.common.verifiers.SymbolVerifier;
import com.proserus.stocks.view.common.verifiers.YearVerifier;
import com.proserus.stocks.view.general.Window;

public class SymbolsModificationView extends AbstractDialog implements ActionListener, FocusListener,Observer {

	private static final String REQUIRED_FIELD_MISSING = "Required field missing";
	private static final String CANNOT_ADD_PRICE = "Cannot add price";
	private JTextField customPrice = new JTextField();
	private JTextField year = new JTextField();
	private JButton add = new JButton("Add");
	private Symbol symbol;
	private AddEditSymbolPanelImpl northPanel;

	private static final String THE_SYMBOL_ALREADY_EXIST = "The symbol already exist";

	private static final String CANNOT_ADD_SYMBOL = "Cannot rename symbol";

	public SymbolsModificationView(Symbol symbol) {
		this.symbol = symbol;
		setModal(true);
		setTitle("Symbol modification");
		setLayout(new BorderLayout());
		setSize(595, 600);
		setResizable(false);
		init();
		PortfolioControllerImpl.getInstance().addSymbolsObserver(this);
	}
	
	public void setSymbol(Symbol s){
		symbol = s;
	}

	private void init() {
		northPanel = new AddEditSymbolPanelImpl(false);
		northPanel.setDisableWhenExist(false);
		add(northPanel, BorderLayout.NORTH);
		// add(new AddHistoricalPricePanelImpl(), BorderLayout.NORTH);

		northPanel.getSymbolField().setText(symbol.getTicker());
		northPanel.getSymbolField().setInputVerifier(new SymbolVerifier());
		northPanel.getCompanyNameField().setText(symbol.getName());
		// TODO Manage Date better
		// northPanel.getPprice.setText(symbol.getPrice(DateUtil.getCurrentYear()).toString());
		northPanel.getUseCustomPriceField().setSelected(symbol.isCustomPriceFirst());

		for (CurrencyEnum cur : CurrencyEnum.values()) {
			northPanel.getCurrencyField().addItem(cur);
		}
		northPanel.getCurrencyField().setMaximumRowCount(11);
		northPanel.getCurrencyField().setSelectedItem(symbol.getCurrency());

		SymbolsModifTable table = new SymbolsModifTable(symbol);
		year.addKeyListener(table);
		add.addKeyListener(table);
		customPrice.addKeyListener(table);

		add(new JScrollPane(table), BorderLayout.CENTER);

		JPanel southPanel = new JPanel();
		add(southPanel, BorderLayout.SOUTH);
		southPanel.setLayout(new GridBagLayout());
		GridBagConstraints gridBagConstraints = new GridBagConstraints();

		gridBagConstraints.fill = GridBagConstraints.NONE;
		gridBagConstraints.weightx = .1;
		gridBagConstraints.anchor = GridBagConstraints.WEST;
		southPanel.add(new JLabel("Year: "), gridBagConstraints);
		gridBagConstraints.fill = GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 0.25;
		// symbol.setInputVerifier(new SymbolVerifier());
		southPanel.add(year, gridBagConstraints);
		year.setInputVerifier(new YearVerifier());

		gridBagConstraints.anchor = GridBagConstraints.CENTER;
		gridBagConstraints.fill = GridBagConstraints.NONE;
		gridBagConstraints.weightx = .1;
		southPanel.add(new JLabel("Custom price: "), gridBagConstraints);
		gridBagConstraints.anchor = GridBagConstraints.WEST;
		gridBagConstraints.fill = GridBagConstraints.BOTH;
		gridBagConstraints.weightx = .25;
		southPanel.add(customPrice, gridBagConstraints);
		customPrice.setInputVerifier(new NumberVerifier());

		gridBagConstraints.fill = GridBagConstraints.VERTICAL;
		gridBagConstraints.anchor = GridBagConstraints.CENTER;
		gridBagConstraints.weightx = .5;
		southPanel.add(add, gridBagConstraints);
		add.addActionListener(this);

		northPanel.getSymbolField().addFocusListener(this);
		northPanel.getCompanyNameField().addFocusListener(this);
		northPanel.getCurrencyField().addFocusListener(this);
		northPanel.getUseCustomPriceField().addFocusListener(this);
	}

	public void windowClosing(WindowEvent e) {
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (!year.getText().isEmpty() && !customPrice.getText().isEmpty()) {
			if(symbol.getMapPrices().containsKey(new Year(Integer.parseInt(year.getText())))){
				JOptionPane.showConfirmDialog(this, "Year " + year.getText() + " already exists", "Error adding Price", JOptionPane.DEFAULT_OPTION,
				        JOptionPane.WARNING_MESSAGE);
				year.setText("");
			}else{
			HistoricalPriceImpl h = new HistoricalPriceImpl();
			h.setCustomPrice(new BigDecimal(Double.parseDouble(customPrice.getText())));
			h.setPrice(BigDecimal.ZERO);
			h.setYear(new Year(Integer.parseInt(year.getText())));
			symbol.addPrice(h);
			PortfolioControllerImpl.getInstance().updateSymbol(symbol);
			year.setText("");
			customPrice.setText("");
			}
		}else{
			JOptionPane.showConfirmDialog(this, REQUIRED_FIELD_MISSING, CANNOT_ADD_PRICE, JOptionPane.DEFAULT_OPTION,
			        JOptionPane.WARNING_MESSAGE);
		}
	}

	@Override
	public void focusLost(FocusEvent e) {
		if (e.getSource().equals(northPanel.getSymbolField()) && !northPanel.getSymbolField().equals(symbol.getTicker())){
			String oldValue = symbol.getTicker();
			symbol.setTicker(northPanel.getSymbolField().getText());
			symbol.setName("");
			//TODO do not compare like this ==> Fixed
			if(!PortfolioControllerImpl.getInstance().addSymbol(symbol).equals(symbol)){
				JOptionPane.showConfirmDialog(Window.getInstance(), THE_SYMBOL_ALREADY_EXIST, CANNOT_ADD_SYMBOL,
				        JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE);
				symbol.setTicker(oldValue);
				northPanel.getSymbolField().setText(oldValue);
			}
		} else {
			if (e.getSource().equals(northPanel.getCompanyNameField()) && !northPanel.getCompanyNameField().equals(symbol.getName())){
				symbol.setName(northPanel.getCompanyNameField().getText());
				PortfolioControllerImpl.getInstance().updateSymbol(symbol);
			}else if (e.getSource().equals(northPanel.getUseCustomPriceField()) && !northPanel.getUseCustomPriceField().equals(symbol.isCustomPriceFirst())){
				symbol.setCustomPriceFirst(northPanel.getUseCustomPriceField().isSelected());
				PortfolioControllerImpl.getInstance().updateSymbol(symbol);
			}else if (e.getSource().equals(northPanel.getCurrencyField()) && !northPanel.getCurrencyField().equals(symbol.getCurrency())){
				symbol.setCurrency((CurrencyEnum)northPanel.getCurrencyField().getSelectedItem());
				PortfolioControllerImpl.getInstance().updateSymbol(symbol);
			}
		}
	}

	@Override
	public void focusGained(FocusEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
    public void update(Observable o, Object arg) {
		if (o instanceof SymbolsBp) {
			for(Symbol s: ((SymbolsBp)o).get()){
				if(s.equals(symbol)){
					symbol = s;
					northPanel.getCompanyNameField().setText(s.getName());
					northPanel.getUseCustomPriceField().setSelected(s.isCustomPriceFirst());
					northPanel.getCurrencyField().setSelectedItem(s.getCurrency());
					break;
				}
			}
		}
	    
    }
}

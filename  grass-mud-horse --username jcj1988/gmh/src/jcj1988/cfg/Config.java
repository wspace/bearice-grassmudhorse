package jcj1988.cfg;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


import jcj1988.Factory.FacArg;
import jcj1988.Factory.Factoryable;
import jcj1988.oprate.Oprateable;
import jcj1988.vm.TriNode;

/**
 * 从xml中获取配置设置
 * */
public final class Config {
	DomParse dp = null;
	Document doc = null;
	NodeList root = null;
	TriNode<Oprateable> tree = new TriNode<Oprateable>(null);
	VMarg vmarg = null;
	Factoryable<FacArg> fct = null;

	public Config(String xmlName, Factoryable<FacArg> f) {
		fct = f;
		dp = new DomParse(xmlName);
		doc = dp.getDocument();
		root = dp.getBoot().item(0).getChildNodes();

		Node n = null;
		// NodeList nl = null;
		for (int i = 0; i < root.getLength(); i++) {
			n = root.item(i);
			if (n.getNodeName().equals("SOURSE")) {
				// TODO: 以后再说
				n.getChildNodes();
			} else if (n.getNodeName().equals("VMARG")) {// 根据xml获得虚拟机配置
				setVMarg(n);
			} else if (n.getNodeName().equals("CMD"))
				buildCmdTree(n, tree);// 创建一个命令树
		}
	}

	private void setVMarg(Node n) {
		int heapMax = 0;
		int s = 0;
		int t = 0;
		int l = 0;

		try {
			heapMax = Integer.parseInt(n.getAttributes()
					.getNamedItem("HEAPMAX").getNodeValue());
		} catch (Exception e) {
			heapMax = 65536;
		}

		NodeList nl = n.getChildNodes();
		try {
			for (int j = 0; j < nl.getLength(); j++) {
				n = nl.item(j);
				if (n.getNodeType() == Node.ELEMENT_NODE) {
					if (n.getNodeName().equals("S")) {
						s = Integer.parseInt(n.getTextContent());
					} else if (n.getNodeName().equals("T")) {
						t = Integer.parseInt(n.getTextContent());
					} else if (n.getNodeName().equals("L")) {
						l = Integer.parseInt(n.getTextContent());
					}
				}
			}
		} catch (Exception e) {
			s = 33609;
			t = 27877;
			l = 39532;
		}

		vmarg = new VMarg(heapMax, s, t, l);

	}

	private void buildCmdTree(Node cmd, TriNode<Oprateable> tr) {// 通过xml的节点创建一个命令树
		Node n = null;
		NodeList nl = cmd.getChildNodes();
		boolean hasChild=false;// 记录是否有孩子
		for (int i = 0; i < nl.getLength(); i++) {
			n = nl.item(i);
			//System.out.println(n.getNodeName());
			TriNode<Oprateable> tn = new TriNode<Oprateable>(null);
			if (n.getNodeType() == Node.ELEMENT_NODE) {		
				if (n.getNodeName().equals("S")) {
					tr.setLeft(tn);
					buildCmdTree(n, tn);
					hasChild=true;
				} else if (n.getNodeName().equals("T")) {
					tr.setMiddle(tn);
					buildCmdTree(n, tn);
					hasChild=true;
				} else if (n.getNodeName().equals("L")) {
					tr.setRight(tn);
					buildCmdTree(n, tn);
					hasChild=true;
				}
			}
		}
		if (!hasChild) {
			tr.setElem(fct.Factory(cmd.getTextContent()));
		}

	}

	public VMarg getVMarg() {
		return vmarg;
	}

	public TriNode<Oprateable> getTree() {
		return tree;
	}

	public static void main(String args[]) {
		Config c = new Config("./bin/jcj1988/cfg/config.xml", null);
		//TriNode.show(c.getTree());
	}
}

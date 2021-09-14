package com.edu.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Vector;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

public class SftpService {
	private static String SFTPHOST = "192.168.0.1";
	private static int SFTPPORT = 22;
	private static String SFTPUSER = "user_sftp";
	private static String SFTPPASS = "password_sftp";
	private static String SFTPWORKINGDIR = "/home/sims/test sftp connection";

	public static void main(String[] args) {
		Session session = null;
		Channel channel = null;
		ChannelSftp channelSftp = null;
		System.out.println("preparing the host information for sftp.");
		try {
			JSch jsch = new JSch();
			session = jsch.getSession(SFTPUSER, SFTPHOST, SFTPPORT);
			session.setPassword(SFTPPASS);
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.connect();
			if (session.isConnected()) {
				System.out.println("Host connected.");
			}
			channel = session.openChannel("sftp");
			channel.connect();
			if (channel.isConnected()) {
				System.out.println("sftp channel opened and connected.");
			}
			channelSftp = (ChannelSftp) channel;

			String serverPath = SFTPWORKINGDIR;
			String localPath = "D:/EDU/test sftp connection/fs/";

			// Upload to Server
//			new SftpService().recursiveFolderUpload(channelSftp, localPath, serverPath);

			// Download from Server
//			new SftpService().recursiveFolderDownload(channelSftp, sourcePath, destinationPath);

			channel.disconnect();
			channelSftp.disconnect();
			if (!channel.isConnected()) {
				System.out.println("Channel disconnect");
			}
			if (!channelSftp.isConnected()) {
				System.out.println("Channel SFTP disconnect");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * recursive Upload File
	 * 
	 * @param sourcePath
	 * @param destinationPath
	 * @throws SftpException
	 * @throws FileNotFoundException
	 */
	private void recursiveFolderUpload(ChannelSftp channelSftp, String localPath, String serverPath)
			throws SftpException, FileNotFoundException {
		File sourceFile = new File(localPath);
		if (sourceFile.isFile()) {
			// copy if it is a file
			channelSftp.cd(serverPath);
			if (!sourceFile.getName().startsWith("."))
				channelSftp.put(new FileInputStream(sourceFile), sourceFile.getName(), ChannelSftp.OVERWRITE);
		} else {
			System.out.println("inside else " + sourceFile.getName());
			File[] files = sourceFile.listFiles();
			if (files != null && !sourceFile.getName().startsWith(".")) {
				channelSftp.cd(serverPath);
				SftpATTRS attrs = null;
				// check if the directory is already existing
				try {
					attrs = channelSftp.stat(serverPath + "/" + sourceFile.getName());
				} catch (Exception e) {
					System.out.println(serverPath + "/" + sourceFile.getName() + " not found");
				}
				// else create a directory
				if (attrs != null) {
					System.out.println("Directory exists IsDir=" + attrs.isDir());
				} else {
					System.out.println("Creating dir " + sourceFile.getName());
					channelSftp.mkdir(sourceFile.getName());
				}
				for (File f : files) {
					recursiveFolderUpload(channelSftp, f.getAbsolutePath(), serverPath + "/" + sourceFile.getName());
				}
			}
		}
	}

	/**
	 * Recursive Download File
	 * 
	 * @param channelSftp
	 * @param sourcePath
	 * @param destinationPath
	 * @throws SftpException
	 */
	@SuppressWarnings("unchecked")
	private void recursiveFolderDownload(ChannelSftp channelSftp, String sourcePath, String destinationPath)
			throws SftpException {

		Vector<ChannelSftp.LsEntry> fileAndFolderList = channelSftp.ls(sourcePath); // Let list of folder content
		// Iterate through list of folder content
		for (ChannelSftp.LsEntry item : fileAndFolderList) {
			// Check if it is a file (not a directory).
			if (!item.getAttrs().isDir()) {
				// Download only if changed later.
				if (!(new File(destinationPath + "/" + item.getFilename())).exists() || (item.getAttrs()
						.getMTime() > Long.valueOf(
								new File(destinationPath + "/" + item.getFilename()).lastModified() / (long) 1000)
								.intValue())) {

					System.out.println(item.getAttrs().getMTime());
					System.out.println(Long
							.valueOf(new File(destinationPath + "/" + item.getFilename()).lastModified() / (long) 1000)
							.intValue());
					new File(destinationPath + "/" + item.getFilename());
					// Download file from source (sourcefilename, destination filename).
					channelSftp.get(sourcePath + "/" + item.getFilename(), destinationPath + "/" + item.getFilename());
				}
			} else if (!(".".equals(item.getFilename()) || "..".equals(item.getFilename()))) {
				new File(destinationPath + "/" + item.getFilename()).mkdirs(); // Empty folder copy.
				// Enter found folder on server to read its contents and create locally.

				String newSourchPath = sourcePath + "/" + item.getFilename();
				String newDestinationPath = destinationPath + "/" + item.getFilename();
				recursiveFolderDownload(channelSftp, newSourchPath, newDestinationPath);
			}
		}
	}
}

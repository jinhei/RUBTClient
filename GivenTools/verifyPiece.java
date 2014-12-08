
///-------- my junk code---------//

import java.io.*;
import java.util.*;

import GivenTools.TorrentInfo;

public class verifyPiece
{
	public static boolean[] verifyPieceInOutput(TorrentInfo torrentInfo, File outputFile) throws IOException
	{
		int totalPieces = torrentInfo.piece_hashes.length;
		int pieceLength = torrentInfo.piece_length;
		int fileSize = torrentInfo.file_length;
		int lastPieceSize = 0;
		boolean[] verifiedPieces = new boolean[totalPieces];
		byte[] pieceArray = null; 
		RandomAccessFile outputFileCheck = new RandomAccessFile(outputFile, "r");

		if(fileSize%totalPieces == 0)
			lastPieceSize = pieceLength;
		else
			lastPieceSize = fileSize%pieceLength;
		
		for (int index = 0; index <totalPieces; index ++)
		{
			//Check if piece is in output file byte array
			if(index < totalPieces - 1)
			{
				pieceArray = new byte[pieceLength];
				outputFileCheck.seek(pieceLength * index + 0);
				outputFileCheck.read(pieceArray);
				outputFileCheck.close();
				
			}
			else
			{
				pieceArray = new byte[lastPieceSize];
				outputFileCheck.seek(lastPieceSize * index + 0);
				outputFileCheck.read(pieceArray);
				outputFileCheck.close();
			}
			
			//Verify SHA1
			if(Client.verifySHA1(pieceArray))
					verifiedPieces[index] = true;

			}
			

		return verifiedPieces;
	}
}

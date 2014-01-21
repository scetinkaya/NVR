function assembleMyInput

dbstop if error;
dbstop if warning;

rdcFilename1                                   = 'N-H_medium1.m'; 
combinedResonancesAndProtonCoordinatesFilename = 'combinedResonancesAndProtonCoordinates.txt';  
dsspFilename                                   ='model_1BVH_1_withHydrogens.parsedDSSP'; 
nhVectorsFilename                              = 'N-H_vectors.m';  
outfilename                                    = 'myinput.m';



nhRDCs                                         = load(rdcFilename1);


[RESNUMS resonanceAA_Name H_CS N_CS protonX protonY protonZ] ...
    = textread(combinedResonancesAndProtonCoordinatesFilename,'%d %s %f %f %f %f %f');

[resIndex1 resIndex2 dsspType] = textread(dsspFilename, '%d %d %s');


nhVectors   = load(nhVectorsFilename);


for k=1:resIndex1(1)-1
    ss(k,1) = 'C';
end
    for j=1:size(resIndex1,1)
        for k=resIndex1(j):resIndex2(j)
            ss(k,1) = determineDsspType(dsspType(j));
        end
        if j < size(resIndex1,1)
            c = resIndex1(j+1);
        else
            c = length(RESNUMS);
        end
        for k=resIndex2(j)+1:c-1
            ss(k,1) = 'C';
        end
    end
    ss(c,1) = 'C';
 for j=(size(nhRDCs,1)+1):size(RESNUMS,1)
    nhRDCs(j,2)=-999;
 end   
fprintf(1, 'check out %s\n', outfilename);
fid         = fopen(outfilename, 'w');


    
for i = 1:size(RESNUMS,1)
    [nhVector_Index,c] = find(nhVectors(:,1) == RESNUMS(i));
    [nhRDCs_Index,c] = find(nhRDCs(:,1) == RESNUMS(i));
    if (RESNUMS(i) ~= 0) && (size(ss,1)>=RESNUMS(i))
        secondaryStr = ss(RESNUMS(i));
    else
        secondaryStr = 'C';
    end
    if (isempty(nhVector_Index))
        fprintf(fid, '%d\t%s\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%s\t%s\t%d\t%f\t%f\t%f\n', RESNUMS(i), resonanceAA_Name{i}, nhRDCs(nhRDCs_Index,2), -999, ...
		    -999,-999,-999,H_CS(i),N_CS(i),secondaryStr, 'Y',0,protonX(i),protonY(i),protonZ(i));
    else
        fprintf(fid, '%d\t%s\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%s\t%s\t%d\t%f\t%f\t%f\n', RESNUMS(i), resonanceAA_Name{i}, nhRDCs(nhRDCs_Index,2), -999, ...
		    nhVectors(nhVector_Index,2),nhVectors(nhVector_Index,3),nhVectors(nhVector_Index,4),H_CS(i),N_CS(i),secondaryStr, 'Y',0,protonX(i),protonY(i),protonZ(i));
    end
    

%   if (i<=size(nhVectors,1))
%       fprintf(fid, '%d\t%s\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%s\t%s\t%d\t%f\t%f\t%f\n', RESNUMS(i), resonanceAA_Name{i}, nhRDCs(i,2), -999, ...
% 		    nhVectors(i,2),nhVectors(i,3),nhVectors(i,4),H_CS(i),N_CS(i),ss(i), 'Y',0,protonX(i),protonY(i),protonZ(i));
%   else
%       fprintf(fid, '%d\t%s\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%s\t%s\t%d\t%f\t%f\t%f\n', RESNUMS(i), resonanceAA_Name{i}, nhRDCs(i,2), -999, ...
% 		    -999,-999,-999,H_CS(i),N_CS(i),ss(i), 'Y',0,protonX(i),protonY(i),protonZ(i));
%   end


end

fclose(fid);

% fprintf(1, 'enter return to exit debug mode.\n');
% keyboard
end
		
function dsspType = determineDsspType(dsspType)
if strcmp(dsspType,'E') || (strcmp(dsspType,'B'))
     dsspType = 'B';
 else if (strcmp(dsspType,'G'))|| (strcmp(dsspType,'I')) || (strcmp(dsspType,'H'))
         dsspType = 'H';
     else if (strcmp(dsspType,'T')) || (strcmp(dsspType,'S')) || (strcmp(dsspType,'C'))
             dsspType = 'C';
         end
     end
end
end


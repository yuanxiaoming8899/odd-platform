import React, { type FC } from 'react';
import { Typography } from '@mui/material';
import { Markdown } from 'components/shared/elements';

interface DatasetFieldDescriptionPreviewProps {
  isDescriptionEmpty: boolean;
  value: string;
}

const DatasetFieldDescriptionPreview: FC<DatasetFieldDescriptionPreviewProps> = ({
  isDescriptionEmpty,
  value,
}) => (
  <div>
    {!isDescriptionEmpty ? (
      <Markdown value={value} variant='subtitle1' />
    ) : (
      <Typography mt={1} variant='subtitle1'>
        Description is not created yet
      </Typography>
    )}
  </div>
);

export default DatasetFieldDescriptionPreview;
